package no.entur.antu.validation.validator.interchange.duplicate;

import java.util.List;
import no.entur.antu.validation.ValidationError;

public record DuplicateInterchangesError(
  RuleCode ruleCode,
  String interchangeId,
  List<String> duplicateInterchangeIds
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return (
      ruleCode.getErrorMessage() +
      " at " +
      String.join(", ", duplicateInterchangeIds)
    );
  }

  @Override
  public String getEntityId() {
    return interchangeId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    DUPLICATE_INTERCHANGES("Duplicate interchanges found");

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
