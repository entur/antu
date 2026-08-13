package no.entur.antu.pipeline;

import no.entur.antu.exception.RetryableAntuException;
import org.entur.netex.validation.exception.RetryableNetexValidationException;
import org.redisson.client.RedisException;

/**
 * Tells a transient failure, where redelivering the message is the right answer, from a permanent one,
 * where it would only fail the same way and the finding belongs in the report instead.
 */
final class RetryableFailures {

  private RetryableFailures() {}

  /**
   * The whole cause chain is examined: a retryable failure deep in a validator surfaces wrapped.
   *
   * <p>An interrupt is re-flagged on the current thread before returning, because testing for it here is
   * the only place that sees it and the caller is about to unwind.
   */
  static boolean isRetryable(Throwable throwable) {
    for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
      if (cause instanceof InterruptedException) {
        Thread.currentThread().interrupt();
        return true;
      }
      if (
        cause instanceof RetryableNetexValidationException ||
        cause instanceof RetryableAntuException ||
        // Meant to be temporary when it went in (#700): retries every Redis failure because the client
        // does not say which of them are transient.
        cause instanceof RedisException
      ) {
        return true;
      }
    }
    return false;
  }
}
