package no.entur.antu.validation;

import java.util.Map;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReport;

/**
 * Validate a NeTEx dataset in multiple stages: first NeTEx schema validation, then NeTEx validators applied to each
 * NeTEx file, and finally dataset validators applied to the whole dataset.
 * Each step is optional.
 */
public class NetexValidationWorkflow {

  private final Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners;
  private final boolean skipSchemaValidation;
  private final boolean skipNetexValidators;

  public NetexValidationWorkflow(
    Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners,
    boolean skipSchemaValidation,
    boolean skipNetexValidators
  ) {
    this.netexValidatorsRunners = netexValidatorsRunners;
    this.skipSchemaValidation = skipSchemaValidation;
    this.skipNetexValidators = skipNetexValidators;
  }

  public boolean hasSchemaValidator(String validationProfile) {
    return (
      !skipSchemaValidation &&
      getNetexValidatorsRunner(validationProfile).hasSchemaValidator()
    );
  }

  public boolean hasNetexValidators(String validationProfile) {
    return (
      !skipNetexValidators &&
      getNetexValidatorsRunner(validationProfile).hasNetexValidators()
    );
  }

  public boolean hasDatasetValidators(String validationProfile) {
    return (
      hasNetexValidators(validationProfile) &&
      getNetexValidatorsRunner(validationProfile).hasDatasetValidators()
    );
  }

  public ValidationReport runSchemaValidation(
    String validationProfile,
    String codespace,
    String validationReportId,
    String filename,
    byte[] fileContent,
    NetexValidationProgressCallBack netexValidationProgressCallBack
  ) {
    return run(
      validationProfile,
      codespace,
      validationReportId,
      filename,
      fileContent,
      skipSchemaValidation,
      true,
      netexValidationProgressCallBack
    );
  }

  public ValidationReport runNetexValidators(
    String validationProfile,
    String codespace,
    String validationReportId,
    String filename,
    byte[] fileContent,
    NetexValidationProgressCallBack netexValidationProgressCallBack
  ) {
    return run(
      validationProfile,
      codespace,
      validationReportId,
      filename,
      fileContent,
      true,
      skipNetexValidators,
      netexValidationProgressCallBack
    );
  }

  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport runDatasetValidators(
    ValidationReport validationReport,
    String validationProfile,
    NetexValidationProgressCallBack netexValidationProgressCallBack
  ) {
    if (validationReport == null) {
      throw new AntuException("Missing validation report");
    }
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }
    NetexValidatorsRunner netexValidatorsRunner = getNetexValidatorsRunner(
      validationProfile
    );

    return netexValidatorsRunner.runNetexDatasetValidators(
      validationReport,
      netexValidationProgressCallBack
    );
  }

  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @param validationProfile  the NeTEx validation profile
   * @param codespace          the dataset codespace.
   * @param validationReportId the report id.
   * @param filename           the name of the NeTEx file.
   * @param fileContent        the binary content of the NeTEx file.
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  private ValidationReport run(
    String validationProfile,
    String codespace,
    String validationReportId,
    String filename,
    byte[] fileContent,
    boolean skipSchema,
    boolean skipValidators,
    NetexValidationProgressCallBack netexValidationProgressCallBack
  ) {
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }
    if (codespace == null) {
      throw new AntuException("Missing codespace");
    }
    NetexValidatorsRunner netexValidatorsRunner = getNetexValidatorsRunner(
      validationProfile
    );
    return netexValidatorsRunner.validate(
      codespace,
      validationReportId,
      filename,
      fileContent,
      skipSchema,
      skipValidators,
      netexValidationProgressCallBack
    );
  }

  private NetexValidatorsRunner getNetexValidatorsRunner(
    String validationProfile
  ) {
    ValidationProfile profile = ValidationProfile
      .findById(validationProfile)
      .orElseThrow(() ->
        new AntuException("Unknown validation profile: " + validationProfile)
      );
    NetexValidatorsRunner netexValidatorsRunner = netexValidatorsRunners.get(
      profile
    );
    if (netexValidatorsRunner == null) {
      throw new AntuException(
        "Configuration not found for validation profile" + validationProfile
      );
    }
    return netexValidatorsRunner;
  }
}
