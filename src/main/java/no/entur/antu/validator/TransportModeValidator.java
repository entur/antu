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
import org.rutebanken.netex.model.VehicleModeEnumeration;

import java.util.*;
import java.util.function.Predicate;

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
                .filter(Predicate.not(this::validateServiceJourney))
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

    private boolean validateServiceJourney(ServiceJourneyContext serviceJourneyContext) {
        return serviceJourneyContext.scheduledStopPoints.stream()
                .map(commonDataRepository::findStopPlaceId)
                .map(stopPlaceRepository::getTransportModeForStopPlaceId)
                .allMatch(stopPlaceTransportMode -> isValidTransportMode(serviceJourneyContext.transportMode(), stopPlaceTransportMode));
    }

    private boolean isValidTransportMode(AllVehicleModesOfTransportEnumeration serviceJourneyTransportMode, VehicleModeEnumeration stopPlaceTransportMode) {
        if (serviceJourneyTransportMode == null || stopPlaceTransportMode == null) {
            return true;
        } else if ((serviceJourneyTransportMode.equals(AllVehicleModesOfTransportEnumeration.COACH) && stopPlaceTransportMode.equals(VehicleModeEnumeration.BUS))
                || (serviceJourneyTransportMode.equals(AllVehicleModesOfTransportEnumeration.BUS) && stopPlaceTransportMode.equals(VehicleModeEnumeration.COACH))) {
            // Coach and bus are interchangeable
            return true;
        } else if (serviceJourneyTransportMode.equals(AllVehicleModesOfTransportEnumeration.TAXI)
                && (stopPlaceTransportMode.equals(VehicleModeEnumeration.BUS) || stopPlaceTransportMode.equals(VehicleModeEnumeration.COACH))) {
            // Taxi can stop on bus and coach stops
            return true;
        } else {
            return serviceJourneyTransportMode.value().equals(stopPlaceTransportMode.value());
        }
    }

    /*
    private boolean validCombination(TransportModeNameEnum vehicleJourneyTransportMode,
                                     TransportSubModeNameEnum vehicleJourneyTransportSubMode,
                                     TransportModeNameEnum stopMode,
                                     TransportSubModeNameEnum stopSubMode) {

        if (vehicleJourneyTransportMode == null || stopMode == null) {
            return true;
        } else if ((TransportModeNameEnum.Coach == vehicleJourneyTransportMode && TransportModeNameEnum.Bus == stopMode) ||
                (TransportModeNameEnum.Bus == vehicleJourneyTransportMode && TransportModeNameEnum.Coach == stopMode)) {
            // Coach and bus are interchangeable
            return true;
        } else if (TransportModeNameEnum.Taxi == vehicleJourneyTransportMode && (stopMode == TransportModeNameEnum.Bus || stopMode == TransportModeNameEnum.Coach)) {
            return true;
        } else if (vehicleJourneyTransportMode != stopMode) {
            return false;
        } else if (TransportSubModeNameEnum.RailReplacementBus == stopSubMode && vehicleJourneyTransportSubMode != null && TransportSubModeNameEnum.RailReplacementBus != vehicleJourneyTransportSubMode) {
            // Only rail replacement bus service can visit rail replacement bus stops
            return false;
        } else {
            return true;
        }
    }
    */

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
            return transportModeNode == null
                    ? getTransportModeFromLine(serviceJourneyItem, validationContext)
                    : AllVehicleModesOfTransportEnumeration.fromValue(transportModeNode.getStringValue());
        } catch (Exception ex) {
            throw new AntuException(ex);
        }
    }

    private static AllVehicleModesOfTransportEnumeration getTransportModeFromLine(XdmItem serviceJourneyItem,
                                                                                  ValidationContext validationContext) throws SaxonApiException {
        String journeyPatternRef = getJourneyPatternRefFromServiceJourney(serviceJourneyItem);
        String routeRef = getRouteRefForJourneyPatternRef(journeyPatternRef, validationContext);
        String lineRef = getLineRefFromRouteRef(routeRef, validationContext);
        String transportMode = getTransportModeForLineRef(lineRef, validationContext);
        return transportMode == null ? AllVehicleModesOfTransportEnumeration.UNKNOWN : AllVehicleModesOfTransportEnumeration.fromValue(transportMode);
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

    private static String getTransportModeForLineRef(String lineRef,
                                                     ValidationContext validationContext) throws SaxonApiException {
        XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler()
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/lines/Line[@id = '" + lineRef + "']")
                .load();
        selector.setContextItem(validationContext.getXmlNode());
        XdmItem lineItem = selector.evaluateSingle();
        XdmNode transportModeNode = getChild(lineItem.stream().asNode(), new QName("n", Constants.NETEX_NAMESPACE, "TransportMode"));
        return transportModeNode == null ? null : transportModeNode.getStringValue();
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
