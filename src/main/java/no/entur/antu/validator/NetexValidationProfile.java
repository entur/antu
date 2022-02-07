package no.entur.antu.validator;

import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReport;

import java.util.Map;

/**
 * Validate a NeTEx dataset according to a given validation profile.
 * A Validation profile defines the set of rules to be applied during the validation.
 */
public class NetexValidationProfile {

    private final Map<String, NetexValidatorsRunner> netexValidatorsRunners;

    public NetexValidationProfile(Map<String, NetexValidatorsRunner> netexValidatorsRunners) {
        this.netexValidatorsRunners = netexValidatorsRunners;
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
    public ValidationReport validate(String validationProfile, String codespace, String validationReportId, String filename, byte[] fileContent) {
        if (validationProfile == null) {
            throw new AntuException("Missing validation profile ");
        }
        NetexValidatorsRunner netexValidatorsRunner = netexValidatorsRunners.get(validationProfile);
        if (netexValidatorsRunner == null) {
            throw new AntuException("Unknown validation profile " + validationProfile);
        } else {
            return netexValidatorsRunner.validate(codespace, validationReportId, filename, fileContent);
        }
    }
}
