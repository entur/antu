package no.entur.antu.validation.validator.servicejourney.transportmode;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.TransportModeAndSubMode;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the transport mode of a service journey matches the quays it visits.
 * The transport mode of a service journey must be matched for the quays it visits.
 * Chouette reference: 4-VehicleJourney-3
 */
public class MismatchedTransportModeValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    MismatchedTransportModeValidator.class
  );
  private final CommonDataRepository commonDataRepository;

  private final StopPlaceRepository stopPlaceRepository;

  public MismatchedTransportModeValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(validationReportEntryFactory);
    this.stopPlaceRepository = stopPlaceRepository;
    this.commonDataRepository = commonDataRepository;
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return MismatchedTransportModeError.RuleCode.values();
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    XPathValidationContext validationContext
  ) {
    if (validationContext.isCommonFile()) {
      // ServiceJourneys and Line only appear in the Line file.
      return;
    }

    LOGGER.debug("Validating Transport mode");

    MismatchedTransportModeContext.Builder builder =
      new MismatchedTransportModeContext.Builder(validationContext);

    if (!builder.foundTransportModesForLine()) {
      LOGGER.warn(
        "Failed to find the TransportModes for the line in {}, " +
        "skipping the validation of transport modes",
        validationContext.getFileName()
      );
      return;
    }

    builder
      .buildAll()
      .forEach(context ->
        validateServiceJourney(
          context,
          validationReport.getValidationReportId(),
          validationError ->
            addValidationReportEntry(
              validationReport,
              validationContext,
              validationError
            )
        )
      );
  }

  private void validateServiceJourney(
    MismatchedTransportModeContext mismatchedTransportModeContext,
    String validationReportId,
    Consumer<MismatchedTransportModeError> validationError
  ) {
    Function<ScheduledStopPointId, QuayId> findQuayIdForScheduledStopPoint =
      commonDataRepository.hasQuayIds(validationReportId)
        ? scheduledStopPoint ->
          commonDataRepository.findQuayIdForScheduledStopPoint(
            scheduledStopPoint,
            validationReportId
          )
        : mismatchedTransportModeContext::findQuayIdForScheduledStopPoint;

    mismatchedTransportModeContext
      .scheduledStopPointIds()
      .stream()
      .map(findQuayIdForScheduledStopPoint)
      // At this point, we have already validated that all the ids in line file exists,
      // either in the line file or in the common file.
      // So we have probably already have the validation entry for the missing id reference in validation context.
      // So we will simply ignore the null values instead of creating new validation entry.
      .filter(Objects::nonNull)
      .forEach(quayId ->
        validateTransportMode(
          mismatchedTransportModeContext.transportModeAndSubMode(),
          stopPlaceRepository.getTransportModesForQuayId(quayId),
          mismatchedTransportModeContext.serviceJourneyId(),
          validationError
        )
      );
  }

  private void validateTransportMode(
    TransportModeAndSubMode datasetTransportModeAndSubMode,
    TransportModeAndSubMode expectedTransportModeAndSubMode,
    String serviceJourneyId,
    Consumer<MismatchedTransportModeError> validationError
  ) {
    if (
      expectedTransportModeAndSubMode == null ||
      expectedTransportModeAndSubMode.mode() == null ||
      datasetTransportModeAndSubMode == null ||
      datasetTransportModeAndSubMode.mode() == null
    ) {
      // TransportMode on Line is mandatory. At this point, the validation entry for the Missing transport mode,
      // will already be created. So we will simply ignore it, if there is no transportModeForServiceJourney exists.
      // stopPlaceTransportModes should never be null at this point, as it is mandatory in stop places file in tiamat.
      // In worst case we will return true to ignore the validation.
      LOGGER.warn(
        "Transport mode is missing, skipping transport mode validation"
      );
      return;
    }

    // Coach and bus are interchangeable
    if (
      (
        datasetTransportModeAndSubMode
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.COACH) &&
        expectedTransportModeAndSubMode
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.BUS)
      ) ||
      (
        datasetTransportModeAndSubMode
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.BUS) &&
        expectedTransportModeAndSubMode
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.COACH)
      )
    ) {
      return;
    }

    // Taxi can stop on bus and coach stops
    if (
      datasetTransportModeAndSubMode
        .mode()
        .equals(AllVehicleModesOfTransportEnumeration.TAXI) &&
      (
        expectedTransportModeAndSubMode
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.BUS) ||
        expectedTransportModeAndSubMode
          .mode()
          .equals(AllVehicleModesOfTransportEnumeration.COACH)
      )
    ) {
      return;
    }

    if (
      datasetTransportModeAndSubMode
        .mode()
        .equals(expectedTransportModeAndSubMode.mode())
    ) {
      // Only rail replacement bus service can visit rail replacement bus stops
      if (
        expectedTransportModeAndSubMode.subMode() != null &&
        expectedTransportModeAndSubMode
          .subMode()
          .name()
          .equals(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value())
      ) {
        // if the stopPlaceTransportSubMode is RAIL_REPLACEMENT_BUS,
        // then busSubModeForServiceJourney should be RAIL_REPLACEMENT_BUS

        if (
          !datasetTransportModeAndSubMode
            .subMode()
            .name()
            .equals(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value())
        ) {
          validationError.accept(
            new MismatchedTransportModeError(
              MismatchedTransportModeError.RuleCode.INVALID_TRANSPORT_SUB_MODE,
              datasetTransportModeAndSubMode.subMode().name(),
              expectedTransportModeAndSubMode.subMode().name(),
              serviceJourneyId
            )
          );
        }
      }
    } else {
      validationError.accept(
        new MismatchedTransportModeError(
          MismatchedTransportModeError.RuleCode.INVALID_TRANSPORT_MODE,
          datasetTransportModeAndSubMode.mode().value(),
          expectedTransportModeAndSubMode.mode().value(),
          serviceJourneyId
        )
      );
    }
  }
}
