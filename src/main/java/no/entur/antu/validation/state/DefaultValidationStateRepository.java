package no.entur.antu.validation.state;

import java.time.Instant;
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
  public Map<String, ValidationState> allValidationStates() {
    return Map.copyOf(validationStates);
  }

  /**
   * {@code replace} rather than {@code put}, because this is a read-modify-write and the clean-up of a
   * finished validation can land in the middle of it. {@code put} would recreate the entry that clean-up had
   * just removed, and the stalled-validation sweep would then report a timeout for a validation that had
   * already reported ok. On a Redisson map {@code replace} is a server-side conditional write, so a report
   * that has been cleaned up stays cleaned up.
   */
  @Override
  public boolean recordProgress(String validationReportId) {
    ValidationState validationState = validationStates.get(validationReportId);
    if (validationState == null) {
      return false;
    }
    validationState.recordProgressAt(Instant.now());
    return (
      validationStates.replace(validationReportId, validationState) != null
    );
  }

  /**
   * Also conditional, for the same reason: the caller read the state first, so writing it back
   * unconditionally would resurrect a report that finished in between.
   */
  @Override
  public void updateValidationState(
    String validationReportId,
    ValidationState validationState
  ) {
    validationStates.replace(validationReportId, validationState);
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
