package no.entur.antu.memorystore;

/**
 * A repository used to store temporary files created during the validation process.
 */
public interface TemporaryFileRepository {
  /**
   * Upload a file to the temporary file repository
   * @param validationReportId the report id.
   * @param fileName the name of the temporary file.
   * @param content the binary content of the temporary file.
   */
  void upload(String validationReportId, String fileName, byte[] content);

  /**
   * Download a file from the temporary file repository.
   * @param validationReportId the report id.
   * @param fileName the name of the temporary file.
   */
  byte[] download(String validationReportId, String fileName);

  /**
   * Delete all temporary files related to a given validation report.
   * @param reportId the validation report id.
   */
  void cleanUp(String reportId);
}
