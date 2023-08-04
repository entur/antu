package no.entur.antu.validator.servicelinksvalidator;

import no.entur.antu.validator.ValidationError;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

public record ServiceLinksError(
  RuleCode ruleCode,
  double distance,
  ScheduledStopPointRefStructure scheduledStopPointRef,
  String serviceLinkId
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String getEntityId() {
    return serviceLinkId;
  }

  @Override
  public String validationReportEntryMessage() {
    return String.format(
      "%s. ServiceLink = %s, ScheduledStopPointRef = %s, distance = %s",
      ruleCode.getErrorMessage(),
      serviceLinkId(),
      scheduledStopPointRef().getRef(),
      distance()
    );
  }

  enum RuleCode implements no.entur.antu.validator.RuleCode {
    DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT(
      "Distance between stop point and start of linestring, exceeds max limit, in LinkSequenceProjection"
    ),
    DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT(
      "Distance between stop point and end of linestring, exceeds max limit, in LinkSequenceProjection"
    ),
    DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT(
      "Distance between stop point and start of linestring, exceeds warning limit, in LinkSequenceProjection"
    ),
    DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT(
      "Distance between stop point and end of linestring, exceeds warning limit, in LinkSequenceProjection"
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
