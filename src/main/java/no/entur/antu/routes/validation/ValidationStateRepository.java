package no.entur.antu.routes.validation;

import no.entur.antu.config.cache.ValidationState;

/**
 * Store the current state of an in-progress validation.
 *
 */
public interface ValidationStateRepository {
  ValidationState getValidationState(String validationReportId);

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
