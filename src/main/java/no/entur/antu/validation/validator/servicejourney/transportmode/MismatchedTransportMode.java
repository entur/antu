package no.entur.antu.validation.validator.servicejourney.transportmode;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.TransportModes;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the transport mode of a service journey matches the quays it visits.
 * The transport mode of a service journey must be matched for the quays it visits.
 */
public class MismatchedTransportMode extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    MismatchedTransportMode.class
  );
  private final CommonDataRepository commonDataRepository;

  private final StopPlaceRepository stopPlaceRepository;

  public MismatchedTransportMode(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(validationReportEntryFactory);
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return MismatchedTransportModeError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating Transport mode");

    MismatchedTransportModeContext.Builder builder =
      new MismatchedTransportModeContext.Builder(validationContext);

    if (!builder.foundTransportModesForLine()) {
      LOGGER.debug(
        "Failed to find the TransportModes for the line in {}, " +
        "skipping the validation of transport modes",
        validationContext.getFileName()
      );
      return;
    }

    builder
      .buildAll()
      .stream()
      .filter(
        Predicate.not(context ->
          validateServiceJourney(
            context,
            validationReport.getValidationReportId()
          )
        )
      )
      .forEach(context ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new MismatchedTransportModeError(
            MismatchedTransportModeError.RuleCode.INVALID_TRANSPORT_MODE,
            context.transportModes().mode(),
            context.serviceJourneyId()
          )
        )
      );
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    // ServiceJourneys and Line only appear in the Line file.
  }

  private boolean validateServiceJourney(
    MismatchedTransportModeContext mismatchedTransportModeContext,
    String validationReportId
  ) {
    Function<ScheduledStopPointId, QuayId> findQuayIdForScheduledStopPoint =
      commonDataRepository.hasQuayIds(validationReportId)
        ? scheduledStopPoint ->
          commonDataRepository.findQuayIdForScheduledStopPoint(
            scheduledStopPoint,
            validationReportId
          )
        : mismatchedTransportModeContext::findQuayIdForScheduledStopPoint;

    return mismatchedTransportModeContext
      .scheduledStopPointIds()
      .stream()
      .map(findQuayIdForScheduledStopPoint)
      // At this point, we have already validated that all the ids in line file exists,
      // either in the line file or in the common file.
      // So we have probably already have the validation entry for the missing id reference in validation context.
      // So we will simply ignore the null values instead of creating new validation entry.
      .filter(Objects::nonNull)
      .allMatch(quayId ->
        isValidTransportMode(
          mismatchedTransportModeContext.transportModes(),
          stopPlaceRepository.getTransportModesForQuayId(quayId)
        )
      );
  }

  private boolean isValidTransportMode(
    TransportModes datasetTransportModes,
    TransportModes expectedTransportModes
  ) {
    if (
      expectedTransportModes == null ||
      expectedTransportModes.mode() == null ||
      datasetTransportModes == null ||
      datasetTransportModes.mode() == null
    ) {
      // TransportMode on Line is mandatory. At this point, the validation entry for the Missing transport mode,
      // will already be created. So we will simply ignore it, if there is no transportModeForServiceJourney exists.
      // stopPlaceTransportModes should never be null at this point, as it is mandatory in stop places file in tiamat.
      // In worst case we will return true to ignore the validation.
      LOGGER.debug(
        "Transport mode is missing, skipping transport mode validation"
      );
      return true;
    }

    // Coach and bus are interchangeable
    if (
      (
        datasetTransportModes
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.COACH) &&
        expectedTransportModes
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.BUS)
      ) ||
      (
        datasetTransportModes
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.BUS) &&
        expectedTransportModes
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.COACH)
      )
    ) {
      return true;
    }

    // Taxi can stop on bus and coach stops
    if (
      datasetTransportModes
        .mode()
        .equals(AllVehicleModesOfTransportEnumeration.TAXI) &&
      (
        expectedTransportModes
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.BUS) ||
        expectedTransportModes
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.COACH)
      )
    ) {
      return true;
    }

    if (
      datasetTransportModes
        .mode()
        .value()
        .equals(expectedTransportModes.mode().value())
    ) {
      // Only rail replacement bus service can visit rail replacement bus stops
      if (
        expectedTransportModes.subMode() != null &&
        expectedTransportModes
          .subMode()
          .name()
          .equals(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value())
      ) {
        // if the stopPlaceTransportSubMode is RAIL_REPLACEMENT_BUS,
        // then busSubModeForServiceJourney should be RAIL_REPLACEMENT_BUS
        return datasetTransportModes
          .subMode()
          .name()
          .equals(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value());
      } else {
        return true;
      }
    } else {
      return false;
    }
  }
}
