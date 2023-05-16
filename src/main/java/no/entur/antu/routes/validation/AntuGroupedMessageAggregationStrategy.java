package no.entur.antu.routes.validation;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;

import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.VALIDATION_CLIENT_HEADER;
import static no.entur.antu.Constants.VALIDATION_CORRELATION_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER;
import static no.entur.antu.Constants.VALIDATION_PROFILE_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_STAGE_HEADER;
import static no.entur.antu.Constants.VALIDATION_IMPORT_TYPE;

/**
 * Base class for message aggregator containing logic for propagating message headers into the aggregated message.
 */
public abstract class AntuGroupedMessageAggregationStrategy extends GroupedMessageAggregationStrategy {

    /**
     * Copy the validation headers that should be propagated into the aggregated message.
     *
     * @param newExchange
     * @param aggregatedExchange
     */
    protected static void copyValidationHeaders(Exchange newExchange, Exchange aggregatedExchange) {
        aggregatedExchange.getIn().setHeader(VALIDATION_REPORT_ID_HEADER, newExchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER));
        aggregatedExchange.getIn().setHeader(DATASET_CODESPACE, newExchange.getIn().getHeader(DATASET_CODESPACE));
        aggregatedExchange.getIn().setHeader(DATASET_REFERENTIAL, newExchange.getIn().getHeader(DATASET_REFERENTIAL));
        aggregatedExchange.getIn().setHeader(VALIDATION_CORRELATION_ID_HEADER, newExchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER));
        aggregatedExchange.getIn().setHeader(VALIDATION_STAGE_HEADER, newExchange.getIn().getHeader(VALIDATION_STAGE_HEADER));
        aggregatedExchange.getIn().setHeader(VALIDATION_IMPORT_TYPE, newExchange.getIn().getHeader(VALIDATION_IMPORT_TYPE));
        aggregatedExchange.getIn().setHeader(VALIDATION_CLIENT_HEADER, newExchange.getIn().getHeader(VALIDATION_CLIENT_HEADER));
        aggregatedExchange.getIn().setHeader(VALIDATION_PROFILE_HEADER, newExchange.getIn().getHeader(VALIDATION_PROFILE_HEADER));
        aggregatedExchange.getIn().setHeader(VALIDATION_DATASET_FILE_HANDLE_HEADER, newExchange.getIn().getHeader(VALIDATION_DATASET_FILE_HANDLE_HEADER));
    }
}
