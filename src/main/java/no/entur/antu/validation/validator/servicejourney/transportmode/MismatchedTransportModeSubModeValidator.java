package no.entur.antu.validation.validator.servicejourney.transportmode;

import static no.entur.antu.validation.validator.support.NetexUtils.stopPointsInJourneyPattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

/**
 * Validates that the transport mode and sub-mode of a service journey matches the quays it visits.
 * Chouette reference: 4-VehicleJourney-3
 */
public class MismatchedTransportModeSubModeValidator implements JAXBValidator {

  static final ValidationRule RULE_INVALID_TRANSPORT_MODE = new ValidationRule(
    "INVALID_TRANSPORT_MODE",
    "Invalid transport mode",
    "Invalid transport mode: The quay %s accepts %s, but the ServiceJourney %s is defined as %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_INVALID_TRANSPORT_SUB_MODE =
    new ValidationRule(
      "INVALID_TRANSPORT_SUB_MODE",
      "Invalid transport sub-mode",
      "Invalid transport sub-mode: The quay %s accepts %s, but the ServiceJourney %s is defined as %s",
      Severity.ERROR
    );

  /**
   * Iterate through all stop points of all service journeys and compare the transport mode of the associated quay with
   * the transport mode of the service journey.
   */
  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    List<ValidationIssue> issues = new ArrayList<>();

    for (ServiceJourney serviceJourney : validationContext.serviceJourneys()) {
      TransportModeAndSubMode serviceJourneyTransportMode =
        validationContext.transportModeAndSubMode(serviceJourney);

      // skip if neither the service journey nor the line  have a transport mode.
      // this should be validated separately.
      if (serviceJourneyTransportMode == null) {
        continue;
      }

      JourneyPattern journeyPattern = validationContext.journeyPattern(
        serviceJourney
      );
      List<StopPointInJourneyPattern> stopPointInJourneyPatterns =
        stopPointsInJourneyPattern(journeyPattern);

      for (StopPointInJourneyPattern stopPointInJourneyPattern : stopPointInJourneyPatterns) {
        QuayId quayId = validationContext.quayIdForScheduledStopPoint(
          ScheduledStopPointId.of(stopPointInJourneyPattern)
        );

        // skip if the scheduled stop point is not mapped to a quay.
        // this should be validated separately.
        if (quayId == null) {
          continue;
        }
        TransportModeAndSubMode quayTransportModeAndSubMode =
          validationContext.transportModeAndSubModeForQuayId(quayId);

        // skip if the quay does not have a transport mode.
        // this can be caused by a data issue in the external stop register.
        if (quayTransportModeAndSubMode == null) {
          continue;
        }

        validationIssue(
          quayId.id(),
          serviceJourney.getId(),
          quayTransportModeAndSubMode,
          serviceJourneyTransportMode,
          validationContext
        )
          .ifPresent(issues::add);
      }
    }
    return issues;
  }

  /**
   * Return a validation issue if the service journey transport mode/submode does not match
   * the quay transport mode/submode.
   */
  private Optional<ValidationIssue> validationIssue(
    String quayId,
    String serviceJourneyId,
    TransportModeAndSubMode quayTransportModeAndSubMode,
    TransportModeAndSubMode serviceJourneyTransportModeAndSubMode,
    JAXBValidationContext validationContext
  ) {
    if (
      quayTransportModeAndSubMode.mode() !=
      serviceJourneyTransportModeAndSubMode.mode()
    ) {
      // Coach and bus are interchangeable.
      if (
        busServingCoachStop(
          quayTransportModeAndSubMode,
          serviceJourneyTransportModeAndSubMode
        ) ||
        coachServingBusStop(
          quayTransportModeAndSubMode,
          serviceJourneyTransportModeAndSubMode
        )
      ) {
        return Optional.empty();
      }

      // Taxi can stop on bus and coach stops.
      if (
        taxiServingBusOrCoachStop(
          quayTransportModeAndSubMode,
          serviceJourneyTransportModeAndSubMode
        )
      ) {
        return Optional.empty();
      }

      return Optional.of(
        new ValidationIssue(
          RULE_INVALID_TRANSPORT_MODE,
          validationContext.dataLocation(serviceJourneyId),
          quayId,
          quayTransportModeAndSubMode.mode(),
          serviceJourneyId,
          serviceJourneyTransportModeAndSubMode.mode()
        )
      );
    }

    // Only rail replacement bus service can visit rail replacement bus stops.
    if (
      quayTransportModeAndSubMode.subMode() != null &&
      BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS
        .value()
        .equals(quayTransportModeAndSubMode.subMode().name()) &&
      !BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS
        .value()
        .equals(serviceJourneyTransportModeAndSubMode.subMode().name())
    ) {
      return Optional.of(
        new ValidationIssue(
          RULE_INVALID_TRANSPORT_SUB_MODE,
          validationContext.dataLocation(serviceJourneyId),
          quayId,
          quayTransportModeAndSubMode.subMode(),
          serviceJourneyId,
          serviceJourneyTransportModeAndSubMode.subMode()
        )
      );
    }

    return Optional.empty();
  }

  private static boolean taxiServingBusOrCoachStop(
    TransportModeAndSubMode quayTransportModeAndSubMode,
    TransportModeAndSubMode serviceJourneyTransportModeAndSubMode
  ) {
    return (
      serviceJourneyTransportModeAndSubMode.mode() ==
      AllVehicleModesOfTransportEnumeration.TAXI &&
      (
        quayTransportModeAndSubMode.mode() ==
        AllVehicleModesOfTransportEnumeration.BUS ||
        quayTransportModeAndSubMode.mode() ==
        AllVehicleModesOfTransportEnumeration.COACH
      )
    );
  }

  private static boolean coachServingBusStop(
    TransportModeAndSubMode quayTransportModeAndSubMode,
    TransportModeAndSubMode serviceJourneyTransportModeAndSubMode
  ) {
    return (
      quayTransportModeAndSubMode.mode() ==
      AllVehicleModesOfTransportEnumeration.BUS &&
      serviceJourneyTransportModeAndSubMode.mode() ==
      AllVehicleModesOfTransportEnumeration.COACH
    );
  }

  private static boolean busServingCoachStop(
    TransportModeAndSubMode quayTransportModeAndSubMode,
    TransportModeAndSubMode serviceJourneyTransportModeAndSubMode
  ) {
    return (
      quayTransportModeAndSubMode.mode() ==
      AllVehicleModesOfTransportEnumeration.COACH &&
      serviceJourneyTransportModeAndSubMode.mode() ==
      AllVehicleModesOfTransportEnumeration.BUS
    );
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE_INVALID_TRANSPORT_MODE, RULE_INVALID_TRANSPORT_SUB_MODE);
  }
}
