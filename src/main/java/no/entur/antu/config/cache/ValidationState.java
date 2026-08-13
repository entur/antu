package no.entur.antu.config.cache;

import java.time.Instant;
import no.entur.antu.job.ValidationContext;

/**
 * The state of an in-progress validation. Its presence is what tells a redelivered job that the validation
 * is still running, and it is removed when the validation reaches a terminal status.
 *
 * <p>It carries the context because that is the only place the validation client's own identifiers survive
 * once the pipeline has moved on: a validation that stalls still has to be reported back, and the client
 * matches the notification on the correlation id and dataset file handle it sent.
 */
public class ValidationState {

  private ValidationContext context;
  private Instant lastProgressAt;
  private boolean hasErrorInCommonFile;

  public ValidationState() {}

  public ValidationState(ValidationContext context, Instant startedAt) {
    this.context = context;
    this.lastProgressAt = startedAt;
  }

  public ValidationContext getContext() {
    return context;
  }

  /**
   * When this validation last ran a job. Compared against an inactivity threshold to decide that it has
   * stalled, so that a large dataset that is still working through its files is not given up on.
   */
  public Instant getLastProgressAt() {
    return lastProgressAt;
  }

  public void recordProgressAt(Instant now) {
    this.lastProgressAt = now;
  }

  public void setHasErrorInCommonFile(boolean hasErrorInCommonFile) {
    this.hasErrorInCommonFile = hasErrorInCommonFile;
  }

  /**
   * Return true if at least one error or critical validation issue was reported in a common file.
   */
  public boolean hasErrorInCommonFile() {
    return hasErrorInCommonFile;
  }
}
