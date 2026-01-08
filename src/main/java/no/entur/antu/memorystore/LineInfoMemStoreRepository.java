package no.entur.antu.memorystore;

import java.util.List;

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

  /**
   * Retrieve line information for a specific validation report.
   *
   * @param validationReportId the ID of the validation report
   * @return list of line information strings, or null if not found
   */
  List<String> getLineInfo(String validationReportId);

  /**
   * Remove line information for a specific validation report from the repository.
   *
   * @param validationReportId the ID of the validation report
   */
  void removeLineInfo(String validationReportId);
}
