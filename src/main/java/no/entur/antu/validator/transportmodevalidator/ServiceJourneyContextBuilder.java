package no.entur.antu.validator.transportmodevalidator;

import net.sf.saxon.s9api.*;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.TransportModes;
import no.entur.antu.model.TransportSubMode;
import org.entur.netex.validation.Constants;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;

import java.util.List;
import java.util.Objects;

public final class ServiceJourneyContextBuilder {

    record ServiceJourneyContext(
            XdmItem serviceJourneyItem,
            String serviceJourneyId,
            TransportModes transportModes,
            List<String> scheduledStopPoints) {
    }

    private final ValidationContext validationContext;
    private final TransportModes transportModesForLine;

    public ServiceJourneyContextBuilder(ValidationContext validationContext) {
        this.validationContext = validationContext;
        XdmItem lineItem = getRegularLine();
        if (lineItem == null) {
            lineItem = getFlexibleLine();
            verifyFlexibleLineType(lineItem);
        }
        Objects.requireNonNull(lineItem, "Line or FlexibleLine not found.");
        transportModesForLine = findTransportModes(lineItem);
    }

    public ServiceJourneyContext build(XdmItem serviceJourneyItem) {
        return new ServiceJourneyContext(
                serviceJourneyItem,
                serviceJourneyItem.stream().asNode().attribute("id"),
                findTransportModesForServiceJourney(serviceJourneyItem),
                getScheduledStopPointsForServiceJourney(serviceJourneyItem));
    }

    private TransportModes findTransportModesForServiceJourney(XdmItem serviceJourneyItem) {
        TransportModes transportModes = findTransportModes(serviceJourneyItem);
        if (transportModes == null) {
            return transportModesForLine;
        }
        return transportModes;
    }

    private TransportModes findTransportModes(XdmItem item) {
        XdmNode transportModeNode = getChild(
                item.stream().asNode(),
                new QName("n", Constants.NETEX_NAMESPACE, "TransportMode"));

        if (transportModeNode == null) {
            return null;
        }

        AllVehicleModesOfTransportEnumeration transportMode =
                AllVehicleModesOfTransportEnumeration.fromValue(transportModeNode.getStringValue());

        return new TransportModes(
                transportMode,
                transportMode == AllVehicleModesOfTransportEnumeration.BUS
                        ? findBusSubModeForServiceJourney(item)
                        : null
        );
    }

    private List<String> getScheduledStopPointsForServiceJourney(XdmItem serviceJourneyItem) {
        try {
            String journeyPatternRef = getJourneyPatternRefFromServiceJourney(serviceJourneyItem);
            XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                    .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/journeyPatterns/JourneyPattern" +
                             "[@id = '" + journeyPatternRef + "']/pointsInSequence/StopPointInJourneyPattern/ScheduledStopPointRef")
                    .load();
            selector.setContextItem(validationContext.getXmlNode());
            return selector.stream().asListOfNodes().stream()
                    .map(scheduledStopPointRef -> scheduledStopPointRef.attribute("ref"))
                    .toList();
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static String getJourneyPatternRefFromServiceJourney(XdmItem serviceJourneyItem) {
        XdmNode journeyPatternRefNode = getChild(
                serviceJourneyItem.stream().asNode(),
                new QName("n", Constants.NETEX_NAMESPACE, "JourneyPatternRef"));
        return journeyPatternRefNode == null ? null : journeyPatternRefNode.attribute("ref");
    }

    private static TransportSubMode findBusSubModeForServiceJourney(XdmItem item) {
        try {
            XdmNode transportSubModeNode = getChild(
                    item.stream().asNode(),
                    new QName("n", Constants.NETEX_NAMESPACE, "TransportSubmode")
            );

            XdmNode busSubModeNode = transportSubModeNode != null
                    ? getChild(transportSubModeNode, new QName("n", Constants.NETEX_NAMESPACE, "BusSubmode"))
                    : null;

            return busSubModeNode == null ? null : new TransportSubMode(busSubModeNode.getStringValue());

        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private XdmItem getRegularLine() {
        try {
            XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                    .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/lines/Line")
                    .load();
            selector.setContextItem(validationContext.getXmlNode());
            // Considering that lines will only have one Line child.
            return selector.evaluateSingle();
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private XdmItem getFlexibleLine() {
        try {
            XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                    .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/lines/FlexibleLine")
                    .load();
            selector.setContextItem(validationContext.getXmlNode());
            // Considering that lines will only have one FlexibleLine child.
            return selector.evaluateSingle();
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private void verifyFlexibleLineType(XdmItem flexibleLineItem) {
        XdmNode flexibleLineTypeNode = getChild(
                flexibleLineItem.stream().asNode(),
                new QName("n", Constants.NETEX_NAMESPACE, "FlexibleLineType"));

        if (flexibleLineTypeNode == null) {
            throw new AntuException("Missing FlexibleLineType for FlexibleLine");
        }

        FlexibleLineTypeEnumeration flexibleLineType = FlexibleLineTypeEnumeration
                .fromValue(flexibleLineTypeNode.getStringValue());

        if (!flexibleLineType.equals(FlexibleLineTypeEnumeration.FIXED)) {
            throw new AntuException("Unsupported FlexibleLineType in TransportModeValidator: " + flexibleLineType.value());
        }
    }

    private static XdmNode getChild(XdmNode parent, QName childName) {
        XdmSequenceIterator<XdmNode> iter = parent.axisIterator(Axis.CHILD, childName);
        if (iter.hasNext()) {
            return iter.next();
        } else {
            return null;
        }
    }
}
