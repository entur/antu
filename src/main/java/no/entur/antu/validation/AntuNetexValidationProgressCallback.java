package no.entur.antu.validation;

import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.ValidationCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records what the validators of an in-progress validation report back.
 *
 * <p>Nothing here has to keep the message alive: the PubSub streaming pull client extends the ack deadline
 * by itself while a message is being processed. What a progress notification does have to do is keep the
 * validation from looking abandoned, because {@code StalledValidationSweeper} concludes any validation whose
 * last progress is old enough. Without this, the only progress recorded is the start of each job, and a
 * single validator running longer than the stall threshold would be given up on while it was still working.
 *
 * <p>{@code NetexValidatorsRunner} notifies every 10 seconds per running task, but stops after 180 of them.
 * A single task stuck for more than half an hour therefore does fall silent, so the stall threshold has to
 * stay at or above that to keep meaning what it says.
 */
public class AntuNetexValidationProgressCallback
  implements NetexValidationProgressCallBack {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    AntuNetexValidationProgressCallback.class
  );

  private final ValidationStateRepository validationStateRepository;
  private final String validationReportId;

  public AntuNetexValidationProgressCallback(
    ValidationStateRepository validationStateRepository,
    String validationReportId
  ) {
    this.validationStateRepository = validationStateRepository;
    this.validationReportId = validationReportId;
  }

  @Override
  public void notifyProgress(String message) {
    LOGGER.debug("Netex Validation progress: {}", message);
    validationStateRepository.recordProgress(validationReportId);
  }

  /**
   * An error in a common file makes the line files pointless to validate: their findings would all
   * be consequences of the broken common file. The flag is shared so every pod validating the dataset
   * sees it.
   */
  @Override
  public void notifyValidationComplete(ValidationCompleteEvent event) {
    if (event.hasError() && event.validationContext().isCommonFile()) {
      // The event's own id rather than this callback's field, which it is the same value as. Kept distinct
      // so this reads off the event it was handed.
      String eventReportId = event.validationReportId();
      ValidationState validationState =
        validationStateRepository.getValidationState(eventReportId);
      if (validationState == null) {
        return;
      }
      validationState.setHasErrorInCommonFile(true);
      validationStateRepository.updateValidationState(
        eventReportId,
        validationState
      );
    }
  }
}
