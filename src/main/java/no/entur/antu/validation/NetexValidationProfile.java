package no.entur.antu.validation;

import java.util.Map;
import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.exception.AntuException;
import no.entur.antu.routes.validation.ValidationStateRepository;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReport;
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

  public NetexValidationProfile(
    Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners,
    ValidationStateRepository validationStateRepository,
    boolean skipSchemaValidation,
    boolean skipNetexValidators
  ) {
    this.netexValidatorsRunners = netexValidatorsRunners;
    this.validationStateRepository = validationStateRepository;
    this.skipSchemaValidation = skipSchemaValidation;
    this.skipNetexValidators = skipNetexValidators;
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

    ValidationState validationState =
      validationStateRepository.getValidationState(validationReportId);
    boolean validationAlreadyComplete = false;
    boolean hasErrorInCommonFile = false;
    if (validationState == null) {
      LOGGER.info("The validation is already complete, ignoring");
      validationAlreadyComplete = true;
    } else {
      hasErrorInCommonFile = validationState.hasErrorInCommonFile();
      if (hasErrorInCommonFile) {
        LOGGER.info(
          "The validation failed in common file, ignoring NeTEx validators"
        );
      }
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
}
