package no.entur.antu.rest;

/**
 * Strips line breaks out of request-supplied values on their way into the log.
 *
 * <p>A path variable can carry a percent-encoded newline, and the log is what an operator reconstructs an
 * incident from. Neutralising the break keeps a caller from forging lines that read as though antu wrote
 * them.
 */
final class LogSafe {

  private LogSafe() {}

  static String of(String value) {
    return value == null ? null : value.replaceAll("[\\r\\n]", "_");
  }
}
