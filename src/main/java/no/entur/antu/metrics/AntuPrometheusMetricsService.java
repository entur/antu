package no.entur.antu.metrics;

import static no.entur.antu.Constants.REPORT_CREATION_DATE;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Handler;
import org.apache.camel.Header;
import org.entur.netex.validation.validator.ValidationReport;
import org.springframework.stereotype.Component;

@Component
public class AntuPrometheusMetricsService {

  private static final String METRICS_PREFIX = "app.antu.";
  private static final String VALIDATION_ENTRIES_COUNTER_NAME =
    METRICS_PREFIX + "data.validation.entries";
  private static final String VALIDATION_TIME_TAKEN_COUNTER_NAME =
    METRICS_PREFIX + "data.validation.time.taken";

  private final MeterRegistry meterRegistry;

  public AntuPrometheusMetricsService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Handler
  public void validationReportMetrics(
    ValidationReport validationReport,
    @Header(REPORT_CREATION_DATE) LocalDateTime reportCreationDate
  ) {
    countValidationEntries(validationReport);
    timeTakenToValidateDataSet(validationReport, reportCreationDate);
  }

  private void countValidationEntries(ValidationReport validationReport) {
    validationReport
      .getNumberOfValidationEntriesPerRule()
      .forEach((key, value) -> {
        List<Tag> counterTags = new ArrayList<>();
        counterTags.add(
          new ImmutableTag("Codespace", validationReport.getCodespace())
        );
        counterTags.add(new ImmutableTag("Rule", String.valueOf(key)));
        meterRegistry
          .counter(VALIDATION_ENTRIES_COUNTER_NAME, counterTags)
          .increment(value);
      });
  }

  private void timeTakenToValidateDataSet(
    ValidationReport validationReport,
    LocalDateTime reportCreationDate
  ) {
    long seconds = Duration
      .between(reportCreationDate, LocalDateTime.now())
      .toSeconds();
    List<Tag> counterTags = new ArrayList<>();
    counterTags.add(
      new ImmutableTag("Codespace", validationReport.getCodespace())
    );
    meterRegistry
      .counter(VALIDATION_TIME_TAKEN_COUNTER_NAME, counterTags)
      .increment(seconds);
  }
}
