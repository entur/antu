package no.entur.antu.routes.validation;

import java.util.Map;
import no.entur.antu.config.cache.ValidationState;

public class DefaultValidationStateRepository
  implements ValidationStateRepository {

  private final Map<String, ValidationState> validationStates;

  public DefaultValidationStateRepository(
    Map<String, ValidationState> validationStates
  ) {
    this.validationStates = validationStates;
  }

  @Override
  public ValidationState getValidationState(String validationReportId) {
    return validationStates.get(validationReportId);
  }

  @Override
  public void updateValidationState(
    String validationReportId,
    ValidationState validationState
  ) {
    validationStates.put(validationReportId, validationState);
  }

  @Override
  public void createValidationStateIfMissing(
    String validationReportId,
    ValidationState validationState
  ) {
    validationStates.putIfAbsent(validationReportId, validationState);
  }

  @Override
  public void cleanUp(String validationReportId) {
    validationStates.remove(validationReportId);
  }
}
