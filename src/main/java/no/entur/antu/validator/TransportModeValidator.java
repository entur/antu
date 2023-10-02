package no.entur.antu.validator;

import net.sf.saxon.s9api.*;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stop.model.TransportSubMode;
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

        if (validationContext.isCommonFile()) {
            return;
        }

        List<ServiceJourneyContext> serviceJourneys = getServiceJourneys(validationContext).stream()
                .map(serviceJourneyItem ->
                        new ServiceJourneyContext(
                                serviceJourneyItem,
                                serviceJourneyItem.stream().asNode().attribute("id"),
                                getTransportModeForServiceJourney(serviceJourneyItem, validationContext),
                                getScheduledStopPointsForServiceJourney(serviceJourneyItem, validationContext))
                ).toList();

        serviceJourneys.stream()
                .filter(Predicate.not(serviceJourneyContext ->
                        validateServiceJourney(serviceJourneyContext, validationContext, validationReport.getValidationReportId())))
                .forEach(serviceJourneyContext -> validationReport.addValidationReportEntry(
                        createValidationReportEntry(
                                RULE_CODE_NETEX_TRANSPORT_MODE_1,
                                findDataLocation(validationContext, serviceJourneyContext, validationContext.getFileName()),
                                String.format(
                                        "Invalid transport mode %s found in service journey with id %s",
                                        serviceJourneyContext.transportMode(),
                                        serviceJourneyContext.serviceJourneyId
                                )
                        )
                ));

    }

    private DataLocation findDataLocation(ValidationContext validationContext,
                                          ServiceJourneyContext serviceJourneyContext,
                                          String fileName) {
        return validationContext.getLocalIds().stream()
                .filter(localId -> localId.getId().equals(serviceJourneyContext.serviceJourneyId))
                .findFirst()
                .map(idVersion ->
                        new DataLocation(
                                idVersion.getId(),
                                fileName,
                                idVersion.getLineNumber(),
                                idVersion.getColumnNumber()
                        ))
                .orElse(new DataLocation(serviceJourneyContext.serviceJourneyId(), fileName, 0, 0));
    }

    private boolean validateServiceJourney(ServiceJourneyContext serviceJourneyContext,
                                           ValidationContext validationContext,
                                           String validationReportId) {
        return serviceJourneyContext.scheduledStopPoints.stream()
                .map(scheduledStopPoint -> commonDataRepository.findQuayId(scheduledStopPoint, validationReportId))
                // At this point, we have already validated that all the ids in line file exists either in the line file or in the common file.
                // So we have probably already have the validation entry for the missing id reference in validation context. So we will simply ignore
                // the null values instead of creating new validation entry.
                .filter(Objects::nonNull)
                .allMatch(quayId ->
                        isValidTransportMode(
                                serviceJourneyContext::transportMode,
                                () -> getBusSubmodeForServiceJourney(serviceJourneyContext.serviceJourneyItem, validationContext),
                                () -> stopPlaceRepository.getTransportModeForQuayId(quayId),
                                () -> stopPlaceRepository.getTransportSubModeForQuayId(quayId)
                        )
                );
    }

    private boolean isValidTransportMode(Supplier<AllVehicleModesOfTransportEnumeration> getTransportModeForServiceJourney,
                                         Supplier<BusSubmodeEnumeration> getBusSubModeForServiceJourney,
                                         Supplier<VehicleModeEnumeration> getTransportModeForQuayId,
                                         Supplier<TransportSubMode> getTransportSubModeForQuayId) {

        VehicleModeEnumeration transportModeForQuayId = getTransportModeForQuayId.get();
        AllVehicleModesOfTransportEnumeration transportModeForServiceJourney = getTransportModeForServiceJourney.get();
        if (transportModeForServiceJourney == null || transportModeForQuayId == null) {
            // TransportMode on Line is mandatory. At this point, the validation entry for the Missing transport mode,
            // will already be created. So we will simply ignore it, if there is no transportModeForServiceJourney exists.
            // transportModeForQuayId should never be null at this point, as it is mandatory in stop places file in tiamat.
            // In worst case we will return true to ignore the validation.
            return true;
        }

        // Coach and bus are interchangeable
        if ((transportModeForServiceJourney.equals(AllVehicleModesOfTransportEnumeration.COACH)
                && transportModeForQuayId.equals(VehicleModeEnumeration.BUS))
                || (transportModeForServiceJourney.equals(AllVehicleModesOfTransportEnumeration.BUS)
                && transportModeForQuayId.equals(VehicleModeEnumeration.COACH))) {
            return true;
        }

        // Taxi can stop on bus and coach stops
        if (transportModeForServiceJourney.equals(AllVehicleModesOfTransportEnumeration.TAXI)
                && (transportModeForQuayId.equals(VehicleModeEnumeration.BUS)
                || transportModeForQuayId.equals(VehicleModeEnumeration.COACH))) {
            return true;
        }

        if (transportModeForServiceJourney.value().equals(transportModeForQuayId.value())) {
            TransportSubMode stopPlaceTransportSubMode = getTransportSubModeForQuayId.get();
            BusSubmodeEnumeration busSubModeForServiceJourney = getBusSubModeForServiceJourney.get();

            // Only rail replacement bus service can visit rail replacement bus stops
            if (stopPlaceTransportSubMode != null && stopPlaceTransportSubMode.name().equals(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value())) {
                // if the stopPlaceTransportSubMode is RAIL_REPLACEMENT_BUS, then busSubModeForServiceJourney should be RAIL_REPLACEMENT_BUS
                return busSubModeForServiceJourney == BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    private static BusSubmodeEnumeration getBusSubmodeForServiceJourney(XdmItem serviceJourneyItem,
                                                                        ValidationContext validationContext) {
        try {
            XdmNode transportSubmodeNode = getChild(
                    serviceJourneyItem.stream().asNode(),
                    new QName("n", Constants.NETEX_NAMESPACE, "TransportSubmode") // TODO: Is it correct
            );
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

    protected List<XdmItem> getServiceJourneys(ValidationContext validationContext) {
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
            XdmNode transportModeNode = getChild(
                    serviceJourneyItem.stream().asNode(),
                    new QName("n", Constants.NETEX_NAMESPACE, "TransportMode"));
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
                .compile("PublicationDelivery/dataObjects/CompositeFrame/frames/*/journeyPatterns/" +
                        "JourneyPattern[@id = '" + journeyPatternRef + "']")
                .load();
        selector.setContextItem(validationContext.getXmlNode());
        XdmItem journeyPatternItem = selector.evaluateSingle();
        XdmNode routeNode = getChild(
                journeyPatternItem.stream().asNode(),
                new QName("n", Constants.NETEX_NAMESPACE, "RouteRef"));
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
