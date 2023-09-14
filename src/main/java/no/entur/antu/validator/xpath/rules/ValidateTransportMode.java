package no.entur.antu.validator.xpath.rules;

import net.sf.saxon.s9api.*;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.validation.Constants;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.VehicleModeEnumeration;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ValidateTransportMode extends AbstractXPathValidationRule {

    private record ValidationRuleContext(
            XdmItem serviceJourneyItem,
            String serviceJourneyId,
            AllVehicleModesOfTransportEnumeration transportMode,
            List<String> scheduledStopPoints) {

    }

    private static final String RULE_CODE_NETEX_TRANSPORT_MODE_1 = "NETEX_TRANSPORT_MODE_1";

    private final CommonDataRepository commonDataRepository;

    private final StopPlaceRepository stopPlaceRepository;

    public ValidateTransportMode(CommonDataRepository commonDataRepository,
                                 StopPlaceRepository stopPlaceRepository) {
        this.commonDataRepository = commonDataRepository;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    @Override
    public List<XPathValidationReportEntry> validate(XPathValidationContext xPathValidationContext) {
        String fileName = xPathValidationContext.getFileName();

        List<ValidationRuleContext> validationRuleContexts = getServiceJourneys(xPathValidationContext).stream()
                .map(serviceJourneyItem ->
                        new ValidationRuleContext(
                                serviceJourneyItem,
                                serviceJourneyItem.stream().asNode().attribute("id"),
                                getTransportModeForServiceJourney(serviceJourneyItem, xPathValidationContext),
                                getScheduledStopPointsForServiceJourney(serviceJourneyItem, xPathValidationContext))
                ).toList();

        return validationRuleContexts.stream()
                .filter(Predicate.not(validationRuleContext -> validateServiceJourney(validationRuleContext, xPathValidationContext)))
                .map(validationRuleContext -> new XPathValidationReportEntry(
                                String.format(
                                        "Invalid transport mode %s found in service journey with id %s",
                                        validationRuleContext.transportMode(),
                                        validationRuleContext.serviceJourneyId
                                ),
                                RULE_CODE_NETEX_TRANSPORT_MODE_1,
                                getXdmNodeLocation(fileName, validationRuleContext.serviceJourneyItem.stream().asNode())
                        )
                )
                .toList();
    }

    private boolean validateServiceJourney(ValidationRuleContext validationRuleContext,
                                           XPathValidationContext xPathValidationContext) {
        return validationRuleContext.scheduledStopPoints.stream()
                .map(commonDataRepository::findStopPlaceId)
                .filter(Objects::nonNull)
                .allMatch(stopPlaceId ->
                        isValidTransportMode(
                                validationRuleContext::transportMode,
                                () -> getBusSubmodeForServiceJourney(validationRuleContext.serviceJourneyItem, xPathValidationContext),
                                () -> stopPlaceRepository.getTransportModeForStopPlaceId(stopPlaceId),
                                () -> stopPlaceRepository.getTransportSubModeForStopPlaceId(stopPlaceId)
                        )
                );
    }

    private boolean isValidTransportMode(Supplier<AllVehicleModesOfTransportEnumeration> getTransportModeForServiceJourney,
                                         Supplier<BusSubmodeEnumeration> getBusSubModeForServiceJourney,
                                         Supplier<VehicleModeEnumeration> getTransportModeForStopPlace,
                                         Supplier<String> getTransportSubModeForStopPlace) {

        VehicleModeEnumeration transportModeForStopPlace = getTransportModeForStopPlace.get();
        AllVehicleModesOfTransportEnumeration transportModeForServiceJourney = getTransportModeForServiceJourney.get();
        if (transportModeForServiceJourney == null || transportModeForStopPlace == null) {
            return true;
        }

        // Coach and bus are interchangeable
        if ((transportModeForServiceJourney.equals(AllVehicleModesOfTransportEnumeration.COACH) && transportModeForStopPlace.equals(VehicleModeEnumeration.BUS))
                || (transportModeForServiceJourney.equals(AllVehicleModesOfTransportEnumeration.BUS) && transportModeForStopPlace.equals(VehicleModeEnumeration.COACH))) {
            return true;
        }

        // Taxi can stop on bus and coach stops
        if (transportModeForServiceJourney.equals(AllVehicleModesOfTransportEnumeration.TAXI)
                && (transportModeForStopPlace.equals(VehicleModeEnumeration.BUS) || transportModeForStopPlace.equals(VehicleModeEnumeration.COACH))) {
            return true;
        }

        if (transportModeForServiceJourney.value().equals(transportModeForStopPlace.value())) {
            String stopPlaceTransportSubMode = getTransportSubModeForStopPlace.get();
            BusSubmodeEnumeration busSubModeForServiceJourney = getBusSubModeForServiceJourney.get();
            // Only rail replacement bus service can visit rail replacement bus stops
            return !BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value().equals(stopPlaceTransportSubMode)
                    || busSubModeForServiceJourney == null
                    || BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.equals(busSubModeForServiceJourney);
        } else {
            return false;
        }
    }

    private static BusSubmodeEnumeration getBusSubmodeForServiceJourney(XdmItem serviceJourneyItem,
                                                                        XPathValidationContext xPathValidationContext) {
        try {
            XdmNode transportSubmodeNode = getChild(serviceJourneyItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportSubmode"));
            transportSubmodeNode = transportSubmodeNode != null
                    ? transportSubmodeNode
                    : getTransportSubModeNodeFromLine(serviceJourneyItem, xPathValidationContext);

            XdmNode busSubModeNode = transportSubmodeNode != null
                    ? getChild(transportSubmodeNode, new QName("n", Constants.NETEX_NAMESPACE, "BusSubmode"))
                    : null;

            return busSubModeNode == null ? null : BusSubmodeEnumeration.fromValue(busSubModeNode.getStringValue());

        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private List<String> getScheduledStopPointsForServiceJourney(XdmItem serviceJourneyItem,
                                                                 XPathValidationContext xPathValidationContext) {
        try {
            String journeyPatternRef = getJourneyPatternRefFromServiceJourney(serviceJourneyItem);
            XPathSelector selector = xPathValidationContext.getNetexXMLParser().getXPathCompiler()
                    .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/journeyPatterns/JourneyPattern[@id = '" + journeyPatternRef + "']/pointsInSequence/StopPointInJourneyPattern/ScheduledStopPointRef")
                    .load();
            selector.setContextItem(xPathValidationContext.getXmlNode());
            return selector.stream().asListOfNodes().stream().map(scheduledStopPointRef -> scheduledStopPointRef.attribute("ref")).toList();
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static List<XdmItem> getServiceJourneys(XPathValidationContext xPathValidationContext) {
        try {
            XPathSelector selector = xPathValidationContext.getNetexXMLParser().getXPathCompiler()
                    .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/vehicleJourneys/ServiceJourney")
                    .load();
            selector.setContextItem(xPathValidationContext.getXmlNode());
            return selector.stream().toList();
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static AllVehicleModesOfTransportEnumeration getTransportModeForServiceJourney(XdmItem serviceJourneyItem,
                                                                                           XPathValidationContext xPathValidationContext) {
        try {
            XdmNode transportModeNode = getChild(serviceJourneyItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportMode"));
            transportModeNode = transportModeNode != null
                    ? transportModeNode
                    : getTransportModeNodeFromLine(serviceJourneyItem, xPathValidationContext);

            return transportModeNode != null
                    ? AllVehicleModesOfTransportEnumeration.fromValue(transportModeNode.getStringValue())
                    : AllVehicleModesOfTransportEnumeration.UNKNOWN;
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static XdmNode getTransportModeNodeFromLine(XdmItem serviceJourneyItem,
                                                        XPathValidationContext xPathValidationContext) throws SaxonApiException {
        XdmItem lineItem = findLineItemForServiceJourney(serviceJourneyItem, xPathValidationContext);
        return getChild(lineItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportMode"));
    }

    private static XdmNode getTransportSubModeNodeFromLine(XdmItem serviceJourneyItem,
                                                           XPathValidationContext xPathValidationContext) throws SaxonApiException {
        XdmItem lineItem = findLineItemForServiceJourney(serviceJourneyItem, xPathValidationContext);
        return getChild(lineItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportSubmode"));
    }

    private static XdmItem findLineItemForServiceJourney(XdmItem serviceJourneyItem,
                                                         XPathValidationContext xPathValidationContext) throws SaxonApiException {

        String journeyPatternRef = getJourneyPatternRefFromServiceJourney(serviceJourneyItem);
        String routeRef = getRouteRefForJourneyPatternRef(journeyPatternRef, xPathValidationContext);
        String lineRef = getLineRefFromRouteRef(routeRef, xPathValidationContext);
        XPathSelector selector = xPathValidationContext.getNetexXMLParser().getXPathCompiler()
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/lines/Line[@id = '" + lineRef + "']")
                .load();
        selector.setContextItem(xPathValidationContext.getXmlNode());
        return selector.evaluateSingle();
    }

    private static String getJourneyPatternRefFromServiceJourney(XdmItem serviceJourneyItem) {
        XdmNode journeyPatternRefNode = getChild(
                serviceJourneyItem.stream().asNode(),
                new QName("n", Constants.NETEX_NAMESPACE, "JourneyPatternRef"));
        return journeyPatternRefNode == null ? null : journeyPatternRefNode.attribute("ref");
    }

    private static String getRouteRefForJourneyPatternRef(String journeyPatternRef,
                                                          XPathValidationContext xPathValidationContext) throws SaxonApiException {
        XPathSelector selector = xPathValidationContext.getNetexXMLParser().getXPathCompiler()
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/journeyPatterns/JourneyPattern[@id = '" + journeyPatternRef + "']")
                .load();
        selector.setContextItem(xPathValidationContext.getXmlNode());
        XdmItem journeyPatternItem = selector.evaluateSingle();
        XdmNode routeNode = getChild(journeyPatternItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "RouteRef"));
        return routeNode == null ? null : routeNode.attribute("ref");
    }

    private static String getLineRefFromRouteRef(String routeRef,
                                                 XPathValidationContext xPathValidationContext) throws SaxonApiException {
        XPathSelector selector = xPathValidationContext.getNetexXMLParser().getXPathCompiler()
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/routes/Route[@id = '" + routeRef + "']")
                .load();
        selector.setContextItem(xPathValidationContext.getXmlNode());
        XdmItem routeItem = selector.evaluateSingle();
        XdmNode lineNode = getChild(routeItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "LineRef"));
        return lineNode == null ? null : lineNode.attribute("ref");
    }

    @Override
    public String getCode() {
        return RULE_CODE_NETEX_TRANSPORT_MODE_1;
    }

    @Override
    public String getMessage() {
        return "Invalid transport mode";
    }
}
