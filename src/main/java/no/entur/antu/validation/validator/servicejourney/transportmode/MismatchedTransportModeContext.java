package no.entur.antu.validation.validator.servicejourney.transportmode;

import java.util.List;
import java.util.Optional;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.TransportModeAndSubMode;
import no.entur.antu.model.TransportSubMode;
import org.entur.netex.validation.Constants;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MismatchedTransportModeContext(
  XPathValidationContext validationContext,
  XdmItem serviceJourneyItem,
  String serviceJourneyId,
  TransportModeAndSubMode transportModeAndSubMode,
  List<ScheduledStopPointId> scheduledStopPointIds,
  String pathToFrames
) {
  public QuayId findQuayIdForScheduledStopPoint(
    ScheduledStopPointId scheduledStopPointId
  ) {
    try {
      XPathSelector selector = validationContext
        .getNetexXMLParser()
        .getXPathCompiler()
        .compile(
          pathToFrames +
          "/stopAssignments/PassengerStopAssignment" +
          "/ScheduledStopPointRef[@ref = '" +
          scheduledStopPointId.id() +
          "']"
        )
        .load();
      selector.setContextItem(validationContext.getXmlNode());

      XdmItem scheduledStopPointRefItem = selector.evaluateSingle();
      if (scheduledStopPointRefItem == null) {
        Builder.LOGGER.debug(
          "PassengerStopAssignment not found in line file, for scheduledStopPoint {}",
          scheduledStopPointId.id()
        );
        return null;
      }
      XdmNode passengerStopAssignment = Builder.getParent(
        scheduledStopPointRefItem.stream().asNode(),
        new QName("n", Constants.NETEX_NAMESPACE, "PassengerStopAssignment")
      );
      if (passengerStopAssignment != null) {
        XdmNode quayRef = Builder.getChild(
          passengerStopAssignment,
          new QName("n", Constants.NETEX_NAMESPACE, "QuayRef")
        );
        return Optional
          .ofNullable(quayRef)
          .map(qRef -> qRef.attribute("ref"))
          .map(QuayId::ofValidId)
          .orElse(null);
      }
      return null;
    } catch (Exception ex) {
      throw new AntuException(ex);
    }
  }

  public static final class Builder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private final XPathValidationContext validationContext;
    private final TransportModeAndSubMode transportModeAndSubModeForLine;
    private final String pathToFrames;

    public Builder(XPathValidationContext validationContext) {
      this.validationContext = validationContext;
      this.pathToFrames =
        isCompositeFrameExists()
          ? "PublicationDelivery/dataObjects/CompositeFrame/frames/*"
          : "PublicationDelivery/dataObjects/*";
      XdmItem lineItem = getRegularLine();
      if (lineItem == null) {
        lineItem = getFlexibleLine();
        if (lineItem != null) {
          verifyFlexibleLineType(lineItem);
        }
      }
      if (lineItem != null) {
        transportModeAndSubModeForLine = findTransportModes(lineItem);
      } else {
        transportModeAndSubModeForLine = null;
        LOGGER.debug(
          "Failed to find the Line or FlexibleLine in {}",
          validationContext.getFileName()
        );
      }
    }

    public boolean foundTransportModesForLine() {
      return transportModeAndSubModeForLine != null;
    }

    public List<MismatchedTransportModeContext> buildAll() {
      return getServiceJourneys().stream().map(this::build).toList();
    }

    public MismatchedTransportModeContext build(XdmItem serviceJourneyItem) {
      return new MismatchedTransportModeContext(
        validationContext,
        serviceJourneyItem,
        serviceJourneyItem.stream().asNode().attribute("id"),
        findTransportModesForServiceJourney(serviceJourneyItem),
        getScheduledStopPointsForServiceJourney(serviceJourneyItem),
        pathToFrames
      );
    }

    private TransportModeAndSubMode findTransportModesForServiceJourney(
      XdmItem serviceJourneyItem
    ) {
      TransportModeAndSubMode transportModeAndSubMode = findTransportModes(
        serviceJourneyItem
      );
      if (transportModeAndSubMode == null) {
        return transportModeAndSubModeForLine;
      }
      return transportModeAndSubMode;
    }

    private TransportModeAndSubMode findTransportModes(XdmItem item) {
      XdmNode transportModeNode = getChild(
        item.stream().asNode(),
        new QName("n", Constants.NETEX_NAMESPACE, "TransportMode")
      );

      if (transportModeNode == null) {
        return null;
      }

      AllVehicleModesOfTransportEnumeration transportMode =
        AllVehicleModesOfTransportEnumeration.fromValue(
          transportModeNode.getStringValue()
        );

      return new TransportModeAndSubMode(
        transportMode,
        transportMode == AllVehicleModesOfTransportEnumeration.BUS
          ? findBusSubModeForServiceJourney(item)
          : null
      );
    }

    private static TransportSubMode findBusSubModeForServiceJourney(
      XdmItem item
    ) {
      try {
        XdmNode transportSubModeNode = getChild(
          item.stream().asNode(),
          new QName("n", Constants.NETEX_NAMESPACE, "TransportSubmode")
        );

        XdmNode busSubModeNode = transportSubModeNode != null
          ? getChild(
            transportSubModeNode,
            new QName("n", Constants.NETEX_NAMESPACE, "BusSubmode")
          )
          : null;

        return busSubModeNode == null
          ? null
          : new TransportSubMode(busSubModeNode.getStringValue());
      } catch (Exception ex) {
        throw new AntuException(ex);
      }
    }

    private List<XdmItem> getServiceJourneys() {
      try {
        XPathSelector selector = validationContext
          .getNetexXMLParser()
          .getXPathCompiler()
          .compile(pathToFrames + "/vehicleJourneys/ServiceJourney")
          .load();
        selector.setContextItem(validationContext.getXmlNode());
        return selector.stream().toList();
      } catch (Exception ex) {
        throw new AntuException(ex);
      }
    }

    private List<ScheduledStopPointId> getScheduledStopPointsForServiceJourney(
      XdmItem serviceJourneyItem
    ) {
      try {
        String journeyPatternRef = getJourneyPatternRefFromServiceJourney(
          serviceJourneyItem
        );
        String journeyPatternPath =
          pathToFrames + "/journeyPatterns/JourneyPattern";
        XPathSelector selector = validationContext
          .getNetexXMLParser()
          .getXPathCompiler()
          .compile(
            journeyPatternPath +
            "[@id = '" +
            journeyPatternRef +
            "']/pointsInSequence/StopPointInJourneyPattern/ScheduledStopPointRef"
          )
          .load();
        selector.setContextItem(validationContext.getXmlNode());
        return selector
          .stream()
          .asListOfNodes()
          .stream()
          .map(scheduledStopPointRef -> scheduledStopPointRef.attribute("ref"))
          .map(ScheduledStopPointId::new)
          .toList();
      } catch (Exception ex) {
        throw new AntuException(ex);
      }
    }

    private static String getJourneyPatternRefFromServiceJourney(
      XdmItem serviceJourneyItem
    ) {
      XdmNode journeyPatternRefNode = getChild(
        serviceJourneyItem.stream().asNode(),
        new QName("n", Constants.NETEX_NAMESPACE, "JourneyPatternRef")
      );
      return journeyPatternRefNode == null
        ? null
        : journeyPatternRefNode.attribute("ref");
    }

    private XdmItem getRegularLine() {
      try {
        XPathSelector selector = validationContext
          .getNetexXMLParser()
          .getXPathCompiler()
          .compile(pathToFrames + "/lines/Line")
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
        XPathSelector selector = validationContext
          .getNetexXMLParser()
          .getXPathCompiler()
          .compile(pathToFrames + "/lines/FlexibleLine")
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
        new QName("n", Constants.NETEX_NAMESPACE, "FlexibleLineType")
      );

      if (flexibleLineTypeNode == null) {
        throw new AntuException("Missing FlexibleLineType for FlexibleLine");
      }

      FlexibleLineTypeEnumeration flexibleLineType =
        FlexibleLineTypeEnumeration.fromValue(
          flexibleLineTypeNode.getStringValue()
        );

      if (!flexibleLineType.equals(FlexibleLineTypeEnumeration.FIXED)) {
        throw new AntuException(
          "Unsupported FlexibleLineType in TransportModeValidator: " +
          flexibleLineType.value()
        );
      }
    }

    private static XdmNode getChild(XdmNode parent, QName childName) {
      XdmSequenceIterator<XdmNode> iter = parent.axisIterator(
        Axis.CHILD,
        childName
      );
      if (iter.hasNext()) {
        return iter.next();
      } else {
        return null;
      }
    }

    private static XdmNode getParent(XdmNode parent, QName parentName) {
      XdmSequenceIterator<XdmNode> iter = parent.axisIterator(
        Axis.PARENT,
        parentName
      );
      if (iter.hasNext()) {
        return iter.next();
      } else {
        return null;
      }
    }

    private boolean isCompositeFrameExists() {
      try {
        XPathSelector selector = validationContext
          .getNetexXMLParser()
          .getXPathCompiler()
          .compile("PublicationDelivery/dataObjects/CompositeFrame")
          .load();
        selector.setContextItem(validationContext.getXmlNode());
        return selector.stream().exists();
      } catch (Exception ex) {
        throw new AntuException(ex);
      }
    }
  }
}
