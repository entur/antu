package no.entur.antu.validator;

import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.Exchange;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntuNetexValidationProgressCallback implements NetexValidationProgressCallBack {

    private static final Logger LOGGER = LoggerFactory.getLogger(AntuNetexValidationProgressCallback.class);

    private final BaseRouteBuilder baseRouteBuilder;
    private final Exchange exchange;

    public AntuNetexValidationProgressCallback(BaseRouteBuilder baseRouteBuilder, Exchange exchange) {
        this.baseRouteBuilder = baseRouteBuilder;
        this.exchange = exchange;
    }

    @Override
    public void notifyProgress(String message) {
        LOGGER.debug("Netex Validation progress: {}", message);
        baseRouteBuilder.extendAckDeadline(exchange);
    }
}
