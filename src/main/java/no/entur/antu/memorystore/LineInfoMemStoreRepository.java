package no.entur.antu.memorystore;

/**
 * Repository for storing and retrieving line information during validation.
 * This abstraction encapsulates the distributed cache and locking mechanism.
 */
public interface LineInfoMemStoreRepository {
  /**
   * Add line information to the repository for a specific validation report.
   * This operation is thread-safe and can be called concurrently from multiple pods.
   *
   * @param validationReportId the ID of the validation report
   * @param lineInfoString the string representation of line information
   */
  void addLineInfo(String validationReportId, String lineInfoString);
}
