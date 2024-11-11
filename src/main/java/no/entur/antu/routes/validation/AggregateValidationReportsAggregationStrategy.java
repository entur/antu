package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;

/**
 * Strategy that aggregates the validation reports produced for individual files into a single validation report.
 */
class AggregateValidationReportsAggregationStrategy
  extends GroupedMessageAggregationStrategy {

  @Override
  public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
    if (
      oldExchange == null ||
      oldExchange.getIn().getBody(ValidationReport.class) == null
    ) {
      return newExchange;
    }

    ValidationReport oldValidationReport = oldExchange
      .getIn()
      .getBody(ValidationReport.class);
    ValidationReport newValidationReport = newExchange
      .getIn()
      .getBody(ValidationReport.class);

    List<ValidationReportEntry> validationReportEntries = Stream
      .concat(
        oldValidationReport.getValidationReportEntries().stream(),
        newValidationReport.getValidationReportEntries().stream()
      )
      .toList();

    Map<String, Long> numberOfValidationEntriesPerRule = Stream
      .concat(
        oldValidationReport
          .getNumberOfValidationEntriesPerRule()
          .entrySet()
          .stream(),
        newValidationReport
          .getNumberOfValidationEntriesPerRule()
          .entrySet()
          .stream()
      )
      .collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum)
      );

    oldExchange
      .getIn()
      .setBody(
        new ValidationReport(
          oldExchange.getIn().getHeader(DATASET_CODESPACE, String.class),
          oldExchange
            .getIn()
            .getHeader(VALIDATION_REPORT_ID_HEADER, String.class),
          validationReportEntries,
          numberOfValidationEntriesPerRule
        )
      );

    LocalDateTime reportCreationDate = oldExchange
      .getIn()
      .getHeader(REPORT_CREATION_DATE, LocalDateTime.class);
    if (
      reportCreationDate == null ||
      reportCreationDate.isAfter(oldValidationReport.getCreationDate())
    ) {
      oldExchange
        .getIn()
        .setHeader(REPORT_CREATION_DATE, oldValidationReport.getCreationDate());
    }
    return oldExchange;
  }
}
