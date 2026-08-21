package no.entur.antu.validation;

import java.util.Map;
import java.util.Optional;
import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.exception.AntuException;
import no.entur.antu.job.AntuJob;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate a NeTEx dataset according to a given validation profile.
 * A Validation profile defines the set of rules to be applied during the validation.
 */
public class NetexValidationProfile {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    NetexValidationProfile.class
  );

  private final Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners;
  private final ValidationStateRepository validationStateRepository;
  private final boolean skipSchemaValidation;
  private final boolean skipNetexValidators;
  private final NetexProfileVersionValidator netexProfileVersionValidator;

  public NetexValidationProfile(
    Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners,
    ValidationStateRepository validationStateRepository,
    boolean skipSchemaValidation,
    boolean skipNetexValidators,
    NetexProfileVersionValidator netexProfileVersionValidator
  ) {
    this.netexValidatorsRunners = netexValidatorsRunners;
    this.validationStateRepository = validationStateRepository;
    this.skipSchemaValidation = skipSchemaValidation;
    this.skipNetexValidators = skipNetexValidators;
    this.netexProfileVersionValidator = netexProfileVersionValidator;
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
  public ValidationReport validate(
    String validationProfile,
    String codespace,
    String validationReportId,
    String filename,
    byte[] fileContent,
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

    boolean validationAlreadyComplete = validationAlreadyComplete(
      validationReportId
    );
    if (validationAlreadyComplete) {
      LOGGER.info("The validation is already complete, ignoring");
    } else {
      Optional<ValidationReportEntry> unsupportedVersion =
        netexProfileVersionValidator.validate(filename, fileContent);
      if (unsupportedVersion.isPresent()) {
        return rejectUnsupportedVersion(
          codespace,
          validationReportId,
          filename,
          unsupportedVersion.get()
        );
      }
    }
    boolean hasErrorInCommonFile = validationHasErrorInCommonFile(
      validationReportId
    );
    if (hasErrorInCommonFile) {
      LOGGER.info(
        "The validation failed in common file, ignoring NeTEx validators"
      );
    }
    return netexValidatorsRunner.validate(
      codespace,
      validationReportId,
      filename,
      fileContent,
      skipSchemaValidation || validationAlreadyComplete,
      skipNetexValidators || validationAlreadyComplete || hasErrorInCommonFile,
      netexValidationProgressCallBack
    );
  }

  /**
   * A file declaring a NeTEx version Antu does not support is reported and nothing else is run
   * against it.
   *
   * <p>Rejecting a common file rejects the shared data every line file resolves its references
   * against, so the line files skip the NeTEx validators: every reference into the missing shared
   * data would otherwise be reported as unresolved, which says nothing about the line file. That is
   * the same suppression a common file with a schema error triggers, except that one reaches the
   * validation state through {@link AntuNetexValidationProgressCallback} when the runner completes.
   * Nothing runs the runner here, so the flag has to be set directly.
   */
  private ValidationReport rejectUnsupportedVersion(
    String codespace,
    String validationReportId,
    String filename,
    ValidationReportEntry unsupportedVersion
  ) {
    LOGGER.info(
      "Rejecting NeTEx file {}: {}",
      filename,
      unsupportedVersion.getMessage()
    );
    if (AntuJob.isCommonFile(filename)) {
      markErrorInCommonFile(validationReportId);
    }
    ValidationReport validationReport = new ValidationReport(
      codespace,
      validationReportId
    );
    validationReport.addValidationReportEntry(unsupportedVersion);
    return validationReport;
  }

  private void markErrorInCommonFile(String validationReportId) {
    ValidationState validationState = getValidationState(validationReportId);
    if (validationState == null) {
      return;
    }
    validationState.setHasErrorInCommonFile(true);
    validationStateRepository.updateValidationState(
      validationReportId,
      validationState
    );
  }

  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport validateDataset(
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
        "Unknown validation profile " + validationProfile
      );
    }
    return netexValidatorsRunner;
  }

  private ValidationState getValidationState(String validationReportId) {
    return validationStateRepository.getValidationState(validationReportId);
  }

  private boolean validationAlreadyComplete(String validationReportId) {
    return getValidationState(validationReportId) == null;
  }

  private boolean validationHasErrorInCommonFile(String validationReportId) {
    ValidationState validationState = getValidationState(validationReportId);
    if (validationState != null) {
      return validationState.hasErrorInCommonFile();
    }
    return false;
  }
}
