package no.entur.antu.job;

import no.entur.antu.Constants;

/**
 * The validation status reported back to the validation client. The wire values are the message
 * body on the validation status queue and are matched literally by marduk and kakka.
 */
public enum ValidationStatus {
  STARTED(Constants.STATUS_VALIDATION_STARTED),
  OK(Constants.STATUS_VALIDATION_OK),
  FAILED(Constants.STATUS_VALIDATION_FAILED),
  /**
   * Antu could not finish the validation, as opposed to the dataset being invalid. Consumed by marduk as
   * {@code JobEvent.State.TIMEOUT}.
   */
  TIMEOUT(Constants.STATUS_VALIDATION_TIMEOUT);

  private final String wireValue;

  ValidationStatus(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }
}
