package no.entur.antu.validation.validator.interchange.stoppoints;

import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;

public record StopPointsInVehicleJourneyError(
  RuleCode ruleCode,
  String interchangeId,
  ScheduledStopPointId stopPointName,
  ServiceJourneyId journeyRef
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return (
      String.format(
        "Stop point (%s) is not a part of journey ref (%s).",
        stopPointName,
        journeyRef.id()
      )
    );
  }

  @Override
  public String getEntityId() {
    return interchangeId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF(
      "FromPointRef in interchange is not a part of FromJourneyRef."
    ),
    TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF(
      "ToPointRef in interchange is not a part of ToJourneyRef."
    );

    private final String errorMessage;

    RuleCode(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    @Override
    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
