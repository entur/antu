package no.entur.antu.validator.transportmodevalidator;

import net.sf.saxon.s9api.*;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stop.model.StopPlaceTransportModes;
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

import static no.entur.antu.validator.transportmodevalidator.ServiceJourneyContextBuilder.*;

public class TransportModeValidator extends AbstractNetexValidator {

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

        ServiceJourneyContextBuilder serviceJourneyContextBuilder = new ServiceJourneyContextBuilder(validationContext);

        getServiceJourneys(validationContext).stream()
                .map(serviceJourneyContextBuilder::build)
                .filter(Predicate.not(
                        serviceJourneyContext ->
                                validateServiceJourney(serviceJourneyContext, validationReport.getValidationReportId())))
                .forEach(serviceJourneyContext -> validationReport.addValidationReportEntry(
                        createValidationReportEntry(
                                RULE_CODE_NETEX_TRANSPORT_MODE_1,
                                findDataLocation(validationContext, serviceJourneyContext),
                                String.format(
                                        "Invalid transport mode %s found in service journey with id %s",
                                        serviceJourneyContext.transportModes().mode(),
                                        serviceJourneyContext.serviceJourneyId()
                                )
                        )
                ));
    }

    private DataLocation findDataLocation(ValidationContext validationContext,
                                          ServiceJourneyContext serviceJourneyContext) {
        String fileName = validationContext.getFileName();
        return validationContext.getLocalIds().stream()
                .filter(localId -> localId.getId().equals(serviceJourneyContext.serviceJourneyId()))
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
                                           String validationReportId) {
        return serviceJourneyContext.scheduledStopPoints().stream()
                .map(scheduledStopPoint -> commonDataRepository.findQuayId(scheduledStopPoint, validationReportId))
                // At this point, we have already validated that all the ids in line file exists either in the line file or in the common file.
                // So we have probably already have the validation entry for the missing id reference in validation context. So we will simply ignore
                // the null values instead of creating new validation entry.
                .filter(Objects::nonNull)
                .allMatch(quayId ->
                        isValidTransportMode(
                                serviceJourneyContext.transportModes(),
                                stopPlaceRepository.getTransportModesForQuayId(quayId)
                        )
                );
    }

    private boolean isValidTransportMode(DatasetTransportModes datasetTransportModes,
                                         StopPlaceTransportModes stopPlaceTransportModes) {

        if (stopPlaceTransportModes == null
            || stopPlaceTransportModes.mode() == null
            || datasetTransportModes.mode() == null) {
            // TransportMode on Line is mandatory. At this point, the validation entry for the Missing transport mode,
            // will already be created. So we will simply ignore it, if there is no transportModeForServiceJourney exists.
            // stopPlaceTransportModes should never be null at this point, as it is mandatory in stop places file in tiamat.
            // In worst case we will return true to ignore the validation.
            return true;
        }

        // Coach and bus are interchangeable
        if ((datasetTransportModes.mode().equals(AllVehicleModesOfTransportEnumeration.COACH)
             && stopPlaceTransportModes.mode().equals(VehicleModeEnumeration.BUS))
            || (datasetTransportModes.mode().equals(AllVehicleModesOfTransportEnumeration.BUS)
                && stopPlaceTransportModes.mode().equals(VehicleModeEnumeration.COACH))) {
            return true;
        }

        // Taxi can stop on bus and coach stops
        if (datasetTransportModes.mode().equals(AllVehicleModesOfTransportEnumeration.TAXI)
            && (stopPlaceTransportModes.mode().equals(VehicleModeEnumeration.BUS)
                || stopPlaceTransportModes.mode().equals(VehicleModeEnumeration.COACH))) {
            return true;
        }

        if (datasetTransportModes.mode().value().equals(stopPlaceTransportModes.mode().value())) {
            // Only rail replacement bus service can visit rail replacement bus stops
            if (stopPlaceTransportModes.subMode() != null
                && stopPlaceTransportModes.subMode().name().equals(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value())) {
                // if the stopPlaceTransportSubMode is RAIL_REPLACEMENT_BUS,
                // then busSubModeForServiceJourney should be RAIL_REPLACEMENT_BUS
                return datasetTransportModes.busSubMode() == BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    List<XdmItem> getServiceJourneys(ValidationContext validationContext) {
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
}