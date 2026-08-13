package no.entur.antu.job;

import org.slf4j.MDC;

/**
 * Puts the identity of the current validation into the SLF4J MDC so that every log line emitted
 * while a message is being processed can be traced back to a dataset, report and file.
 *
 * <p>The keys are rendered by the logback JSON encoder. Values are always overwritten, never left
 * behind: PubSub subscriber threads are pooled and reused across messages.
 */
public final class ValidationMdc {

  public static final String CORRELATION_ID = "correlationId";
  public static final String CODESPACE = "codespace";
  public static final String REPORT_ID = "reportId";
  public static final String FILE_NAME = "fileName";

  private ValidationMdc() {}

  public static void set(ValidationContext context) {
    clear();
    if (context == null) {
      return;
    }
    put(CORRELATION_ID, context.correlationId());
    put(CODESPACE, context.referential());
    put(REPORT_ID, context.validationReportId());
  }

  public static void setFileName(String fileName) {
    put(FILE_NAME, fileName);
  }

  public static void clear() {
    MDC.remove(CORRELATION_ID);
    MDC.remove(CODESPACE);
    MDC.remove(REPORT_ID);
    MDC.remove(FILE_NAME);
  }

  private static void put(String key, String value) {
    if (value != null && !value.isEmpty()) {
      MDC.put(key, value);
    }
  }
}
