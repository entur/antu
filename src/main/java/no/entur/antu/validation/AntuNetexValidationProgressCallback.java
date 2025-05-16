package no.entur.antu.validation;

import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.routes.validation.ValidationStateRepository;
import org.apache.camel.Exchange;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.ValidationCompleteEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Callback that tracks progress of an in-progress validation.
 * The callback serves 2 purposes:
 * <ul>
 *     <li>Extending the Google PubSub ACK deadline when a validator is reporting progress,
 *     to prevent PubSub timeout and redelivery</li>
 *     <li>Updating the shared ValidationState with the output of a validator</li>
 * </ul>
 */
public class AntuNetexValidationProgressCallback
  implements NetexValidationProgressCallBack {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    AntuNetexValidationProgressCallback.class
  );

  private final BaseRouteBuilder baseRouteBuilder;
  private final Exchange exchange;
  private final ValidationStateRepository validationStateRepository;

  public AntuNetexValidationProgressCallback(
    BaseRouteBuilder baseRouteBuilder,
    Exchange exchange,
    ValidationStateRepository validationStateRepository
  ) {
    this.baseRouteBuilder = baseRouteBuilder;
    this.exchange = exchange;
    this.validationStateRepository = validationStateRepository;
  }

  /**
   * Extends the PubSub ACK deadline to prevent PubSub redelivery.
   */
  @Override
  public void notifyProgress(String message) {
    LOGGER.debug("Netex Validation progress: {}", message);
    baseRouteBuilder.extendAckDeadline(exchange);
  }

  /**
   * Update the validation state with the result of a validator.
   */
  @Override
  public void notifyValidationComplete(ValidationCompleteEvent event) {
    if (event.hasError() && event.validationContext().isCommonFile()) {
      ValidationState validationState =
        validationStateRepository.getValidationState(
          event.validationReportId()
        );
      validationState.setHasErrorInCommonFile(true);
      validationStateRepository.updateValidationState(
        event.validationReportId(),
        validationState
      );
    }
  }
}
