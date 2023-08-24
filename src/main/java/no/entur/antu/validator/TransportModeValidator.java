package no.entur.antu.validator;

import net.sf.saxon.s9api.*;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.validation.Constants;
import org.entur.netex.validation.validator.AbstractNetexValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.VehicleModeEnumeration;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class TransportModeValidator extends AbstractNetexValidator {

    private record ServiceJourneyContext(
            XdmItem serviceJourneyItem,
            String serviceJourneyId,
            AllVehicleModesOfTransportEnumeration transportMode,
            List<String> scheduledStopPoints) {
    }

    private static final String RULE_CODE_NETEX_TRANSPORT_MODE_1 = "NETEX_TRANSPORT_MODE_1";

    private final CommonDataRepository commonDataRepository;

    private final StopPlaceRepository stopPlaceRepository;

    public TransportModeValidator(ValidationReportEntryFactory validationReportEntryFactory,
                                  CommonDataRepository commonDataRepository,
                                  StopPlaceRepository stopPlaceRepository) {
        super(validationReportEntryFactory);
        this.commonDataRepository = commonDataRepository;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    @Override
    public Set<String> getRuleDescriptions() {
        return Set.of(createRuleDescription(RULE_CODE_NETEX_TRANSPORT_MODE_1, "Invalid transport mode"));
    }

    @Override
    public void validate(ValidationReport validationReport, ValidationContext validationContext) {
        String fileName = validationContext.getFileName();

        List<ServiceJourneyContext> serviceJourneys = getServiceJourneys(validationContext).stream()
                .map(serviceJourneyItem ->
                        new ServiceJourneyContext(
                                serviceJourneyItem,
                                serviceJourneyItem.stream().asNode().attribute("id"),
                                getTransportModeForServiceJourney(serviceJourneyItem, validationContext),
                                getScheduledStopPointsForServiceJourney(serviceJourneyItem, validationContext))
                ).toList();

        serviceJourneys.stream()
                .filter(Predicate.not(serviceJourneyContext -> validateServiceJourney(serviceJourneyContext, validationContext)))
                .forEach(serviceJourneyContext -> validationReport.addValidationReportEntry(
                        createValidationReportEntry(
                                RULE_CODE_NETEX_TRANSPORT_MODE_1,
                                validationContext.getLocalIds().stream()
                                        .filter(localId -> localId.getId().equals(serviceJourneyContext.serviceJourneyId))
                                        .findFirst()
                                        .map(idVersion -> new DataLocation(idVersion.getId(), fileName, idVersion.getLineNumber(), idVersion.getColumnNumber()))
                                        .orElse(new DataLocation(serviceJourneyContext.serviceJourneyId(), fileName, 0, 0)),
                                String.format(
                                        "Invalid transport mode %s found in service journey with id %s",
                                        serviceJourneyContext.transportMode(),
                                        serviceJourneyContext.serviceJourneyId
                                )
                        )
                ));

    }

    private boolean validateServiceJourney(ServiceJourneyContext serviceJourneyContext,
                                           ValidationContext validationContext) {
        return serviceJourneyContext.scheduledStopPoints.stream()
                .map(commonDataRepository::findStopPlaceId)
                .allMatch(stopPlaceId ->
                        isValidTransportMode(
                                serviceJourneyContext::transportMode,
                                () -> getBusSubmodeForServiceJourney(serviceJourneyContext.serviceJourneyItem, validationContext),
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
                                                                        ValidationContext validationContext) {
        try {
            XdmNode transportSubmodeNode = getChild(serviceJourneyItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportSubmode"));
            transportSubmodeNode = transportSubmodeNode != null
                    ? transportSubmodeNode
                    : getTransportSubModeNodeFromLine(serviceJourneyItem, validationContext);

            XdmNode busSubModeNode = transportSubmodeNode != null
                    ? getChild(transportSubmodeNode, new QName("n", Constants.NETEX_NAMESPACE, "BusSubmode"))
                    : null;

            return busSubModeNode == null ? null : BusSubmodeEnumeration.fromValue(busSubModeNode.getStringValue());

        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private List<String> getScheduledStopPointsForServiceJourney(XdmItem serviceJourneyItem,
                                                                 ValidationContext validationContext) {
        try {
            String journeyPatternRef = getJourneyPatternRefFromServiceJourney(serviceJourneyItem);
            XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                    .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/journeyPatterns/JourneyPattern[@id = '" + journeyPatternRef + "']/pointsInSequence/StopPointInJourneyPattern/ScheduledStopPointRef")
                    .load();
            selector.setContextItem(validationContext.getXmlNode());
            return selector.stream().asListOfNodes().stream().map(scheduledStopPointRef -> scheduledStopPointRef.attribute("ref")).toList();
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static List<XdmItem> getServiceJourneys(ValidationContext validationContext) {
        try {
            XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                    .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/vehicleJourneys/ServiceJourney")
                    .load();
            selector.setContextItem(validationContext.getXmlNode());
            return selector.stream().toList();
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static AllVehicleModesOfTransportEnumeration getTransportModeForServiceJourney(XdmItem serviceJourneyItem,
                                                                                           ValidationContext validationContext) {
        try {
            XdmNode transportModeNode = getChild(serviceJourneyItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportMode"));
            transportModeNode = transportModeNode != null
                    ? transportModeNode
                    : getTransportModeNodeFromLine(serviceJourneyItem, validationContext);

            return transportModeNode != null
                    ? AllVehicleModesOfTransportEnumeration.fromValue(transportModeNode.getStringValue())
                    : AllVehicleModesOfTransportEnumeration.UNKNOWN;
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static XdmNode getTransportModeNodeFromLine(XdmItem serviceJourneyItem,
                                                        ValidationContext validationContext) throws SaxonApiException {
        XdmItem lineItem = findLineItemForServiceJourney(serviceJourneyItem, validationContext);
        return getChild(lineItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportMode"));
    }

    private static XdmNode getTransportSubModeNodeFromLine(XdmItem serviceJourneyItem,
                                                           ValidationContext validationContext) throws SaxonApiException {
        XdmItem lineItem = findLineItemForServiceJourney(serviceJourneyItem, validationContext);
        return getChild(lineItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportSubmode"));
    }

    private static XdmItem findLineItemForServiceJourney(XdmItem serviceJourneyItem,
                                                         ValidationContext validationContext) throws SaxonApiException {

        String journeyPatternRef = getJourneyPatternRefFromServiceJourney(serviceJourneyItem);
        String routeRef = getRouteRefForJourneyPatternRef(journeyPatternRef, validationContext);
        String lineRef = getLineRefFromRouteRef(routeRef, validationContext);
        XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/lines/Line[@id = '" + lineRef + "']")
                .load();
        selector.setContextItem(validationContext.getXmlNode());
        return selector.evaluateSingle();
    }

    private static String getJourneyPatternRefFromServiceJourney(XdmItem serviceJourneyItem) {
        XdmNode journeyPatternRefNode = getChild(
                serviceJourneyItem.stream().asNode(),
                new QName("n", Constants.NETEX_NAMESPACE, "JourneyPatternRef"));
        return journeyPatternRefNode == null ? null : journeyPatternRefNode.attribute("ref");
    }

    private static String getRouteRefForJourneyPatternRef(String journeyPatternRef,
                                                          ValidationContext validationContext) throws SaxonApiException {
        XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/journeyPatterns/JourneyPattern[@id = '" + journeyPatternRef + "']")
                .load();
        selector.setContextItem(validationContext.getXmlNode());
        XdmItem journeyPatternItem = selector.evaluateSingle();
        XdmNode routeNode = getChild(journeyPatternItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "RouteRef"));
        return routeNode == null ? null : routeNode.attribute("ref");
    }

    private static String getLineRefFromRouteRef(String routeRef,
                                                 ValidationContext validationContext) throws SaxonApiException {
        XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/routes/Route[@id = '" + routeRef + "']")
                .load();
        selector.setContextItem(validationContext.getXmlNode());
        XdmItem routeItem = selector.evaluateSingle();
        XdmNode lineNode = getChild(routeItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "LineRef"));
        return lineNode == null ? null : lineNode.attribute("ref");
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
