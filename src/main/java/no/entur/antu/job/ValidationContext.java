package no.entur.antu.job;

/**
 * Identifies one dataset validation and travels with every job belonging to it.
 *
 * <p>{@code referential}, {@code datasetFileHandle}, {@code validationProfile}, {@code
 * validationStage}, {@code validationClient}, {@code importType}, {@code fileCreatedTimestamp} and
 * {@code rutebankenFileHandle} come from the validation client and are echoed back verbatim when
 * the status is notified. {@code codespace}, {@code validationReportId} and {@code correlationId}
 * are derived by antu when the validation starts.
 */
public record ValidationContext(
  String referential,
  String codespace,
  String validationReportId,
  String correlationId,
  String validationProfile,
  String validationClient,
  String validationStage,
  String importType,
  String datasetFileHandle,
  String fileCreatedTimestamp,
  String rutebankenFileHandle
) {
  public static Builder builder() {
    return new Builder();
  }

  /**
   * The same request with the three fields antu derives when the validation starts filled in.
   *
   * <p>Written against the canonical constructor rather than by copying field by field, so that adding a
   * component to this record fails to compile here instead of silently dropping it from every context the
   * pipeline passes around.
   */
  public ValidationContext withIdentity(
    String codespace,
    String validationReportId,
    String correlationId
  ) {
    return new ValidationContext(
      referential,
      codespace,
      validationReportId,
      correlationId,
      validationProfile,
      validationClient,
      validationStage,
      importType,
      datasetFileHandle,
      fileCreatedTimestamp,
      rutebankenFileHandle
    );
  }

  public static final class Builder {

    private String referential;
    private String codespace;
    private String validationReportId;
    private String correlationId;
    private String validationProfile;
    private String validationClient;
    private String validationStage;
    private String importType;
    private String datasetFileHandle;
    private String fileCreatedTimestamp;
    private String rutebankenFileHandle;

    public Builder referential(String referential) {
      this.referential = referential;
      return this;
    }

    public Builder codespace(String codespace) {
      this.codespace = codespace;
      return this;
    }

    public Builder validationReportId(String validationReportId) {
      this.validationReportId = validationReportId;
      return this;
    }

    public Builder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public Builder validationProfile(String validationProfile) {
      this.validationProfile = validationProfile;
      return this;
    }

    public Builder validationClient(String validationClient) {
      this.validationClient = validationClient;
      return this;
    }

    public Builder validationStage(String validationStage) {
      this.validationStage = validationStage;
      return this;
    }

    public Builder importType(String importType) {
      this.importType = importType;
      return this;
    }

    public Builder datasetFileHandle(String datasetFileHandle) {
      this.datasetFileHandle = datasetFileHandle;
      return this;
    }

    public Builder fileCreatedTimestamp(String fileCreatedTimestamp) {
      this.fileCreatedTimestamp = fileCreatedTimestamp;
      return this;
    }

    public Builder rutebankenFileHandle(String rutebankenFileHandle) {
      this.rutebankenFileHandle = rutebankenFileHandle;
      return this;
    }

    public ValidationContext build() {
      return new ValidationContext(
        referential,
        codespace,
        validationReportId,
        correlationId,
        validationProfile,
        validationClient,
        validationStage,
        importType,
        datasetFileHandle,
        fileCreatedTimestamp,
        rutebankenFileHandle
      );
    }
  }
}
