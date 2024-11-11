package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy that wait for all individual NeTEx files to be processed.
 * The total number of reports to process is stored in a header that is included in every incoming message.
 */
class CollectIndividualReportsAggregationStrategy
  extends AntuGroupedMessageAggregationStrategy {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    CollectIndividualReportsAggregationStrategy.class
  );

  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
    Exchange aggregatedExchange = super.aggregate(oldExchange, newExchange);
    copyValidationHeaders(newExchange, aggregatedExchange);
    String currentNetexFileNameList = aggregatedExchange.getProperty(
      PROP_DATASET_NETEX_FILE_NAMES_STRING,
      String.class
    );
    String incomingNetexFileName = newExchange
      .getIn()
      .getHeader(NETEX_FILE_NAME, String.class);
    if (currentNetexFileNameList == null) {
      aggregatedExchange.setProperty(
        PROP_DATASET_NETEX_FILE_NAMES_STRING,
        incomingNetexFileName
      );
    } else {
      aggregatedExchange.setProperty(
        PROP_DATASET_NETEX_FILE_NAMES_STRING,
        currentNetexFileNameList + FILENAME_DELIMITER + incomingNetexFileName
      );
    }
    // check if all individual reports have been received
    // checking against the set of distinct file names in order to exclude possible multiple redeliveries of the same report.
    Long nbNetexFiles = newExchange
      .getIn()
      .getHeader(DATASET_NB_NETEX_FILES, Long.class);
    List<Message> aggregatedMessages = aggregatedExchange.getProperty(
      ExchangePropertyKey.GROUPED_EXCHANGE,
      List.class
    );
    Set<String> aggregatedFileNames = aggregatedMessages
      .stream()
      .map(message -> message.getHeader(NETEX_FILE_NAME, String.class))
      .collect(Collectors.toSet());

    if (LOGGER.isTraceEnabled()) {
      String reportId = aggregatedExchange
        .getIn()
        .getHeader(VALIDATION_REPORT_ID_HEADER, String.class);
      String receivedFileNames = Arrays
        .stream(
          aggregatedExchange
            .getProperty(PROP_DATASET_NETEX_FILE_NAMES_STRING, String.class)
            .split(FILENAME_DELIMITER)
        )
        .sorted()
        .collect(Collectors.joining(FILENAME_DELIMITER));
      LOGGER.trace(
        "Received file {} for report {}. All received files: {}",
        incomingNetexFileName,
        reportId,
        receivedFileNames
      );
    }

    if (aggregatedFileNames.size() >= nbNetexFiles) {
      aggregatedExchange.setProperty(
        Exchange.AGGREGATION_COMPLETE_CURRENT_GROUP,
        true
      );
    }
    return aggregatedExchange;
  }
}
