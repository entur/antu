package no.entur.antu.job;

/**
 * Reports the progress of a dataset validation back to the client that requested it.
 */
public interface ValidationStatusNotifier {
  void notifyStatus(ValidationContext context, ValidationStatus status);
}
