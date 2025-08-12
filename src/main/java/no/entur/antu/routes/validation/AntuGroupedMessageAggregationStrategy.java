package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.*;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for message aggregator containing logic for propagating message headers into the aggregated message.
 */
public abstract class AntuGroupedMessageAggregationStrategy
  extends GroupedMessageAggregationStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    AntuGroupedMessageAggregationStrategy.class
  );

  @Override
  public void timeout(Exchange exchange, int index, int total, long timeout) {
    String reportId = exchange
      .getIn()
      .getHeader(VALIDATION_REPORT_ID_HEADER, String.class);
    String referential = exchange
      .getIn()
      .getHeader(DATASET_REFERENTIAL, String.class);
    String jobType = exchange.getIn().getHeader(JOB_TYPE, String.class);
    LOGGER.warn(
      "A timeout occurred during aggregation [reportId = {}, referential = {}, jobType = {}]",
      reportId,
      referential,
      jobType
    );
  }

  /**
   * Copy the validation headers that should be propagated into the aggregated message.
   */
  protected static void copyValidationHeaders(
    Exchange newExchange,
    Exchange aggregatedExchange
  ) {
    aggregatedExchange
      .getIn()
      .setHeader(
        VALIDATION_REPORT_ID_HEADER,
        newExchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        DATASET_CODESPACE,
        newExchange.getIn().getHeader(DATASET_CODESPACE)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        DATASET_REFERENTIAL,
        newExchange.getIn().getHeader(DATASET_REFERENTIAL)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        VALIDATION_CORRELATION_ID_HEADER,
        newExchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        VALIDATION_STAGE_HEADER,
        newExchange.getIn().getHeader(VALIDATION_STAGE_HEADER)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        VALIDATION_IMPORT_TYPE,
        newExchange.getIn().getHeader(VALIDATION_IMPORT_TYPE)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        VALIDATION_CLIENT_HEADER,
        newExchange.getIn().getHeader(VALIDATION_CLIENT_HEADER)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        VALIDATION_PROFILE_HEADER,
        newExchange.getIn().getHeader(VALIDATION_PROFILE_HEADER)
      );
    aggregatedExchange
      .getIn()
      .setHeader(
        VALIDATION_DATASET_FILE_HANDLE_HEADER,
        newExchange.getIn().getHeader(VALIDATION_DATASET_FILE_HANDLE_HEADER)
      );
  }
}
