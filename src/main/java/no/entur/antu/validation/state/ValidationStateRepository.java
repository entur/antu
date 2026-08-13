package no.entur.antu.validation.state;

import java.util.Map;
import no.entur.antu.config.cache.ValidationState;

/**
 * Store the current state of an in-progress validation.
 *
 */
public interface ValidationStateRepository {
  ValidationState getValidationState(String validationReportId);

  /**
   * The state of every validation currently in progress, keyed by report id.
   *
   * <p>Used to find validations that stalled. A validation is only removed from here when it reaches a
   * terminal status, so anything left behind is either running or stuck.
   */
  Map<String, ValidationState> allValidationStates();

  default boolean hasValidationState(String validationReportId) {
    return getValidationState(validationReportId) != null;
  }

  /**
   * Note that this validation just ran a job, and report whether it is still running at all.
   *
   * <p>Both in one call because every job asks both questions: whether it is a redelivery of something
   * already finished, and, if not, that the validation is still alive so the stalled-validation sweep
   * leaves it alone.
   *
   * @return false if the validation has already reached a terminal status.
   */
  boolean recordProgress(String validationReportId);

  void updateValidationState(
    String validationReportId,
    ValidationState validationState
  );

  void createValidationStateIfMissing(
    String validationReportId,
    ValidationState validationState
  );

  /**
   * Clean up the NeTEx data repository for the given validation report.
   */
  void cleanUp(String validationReportId);
}
