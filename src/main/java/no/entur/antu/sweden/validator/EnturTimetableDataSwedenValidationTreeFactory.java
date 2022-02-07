package no.entur.antu.sweden.validator;

import no.entur.antu.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.ValidationRule;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.rules.ValidateAtLeastOne;

import java.util.List;

/**
 * XPath validation tree for timetable data from Sweden.
 * Validation rules are adapted to match swedish content.
 */
public class EnturTimetableDataSwedenValidationTreeFactory extends EnturTimetableDataValidationTreeFactory {
    public EnturTimetableDataSwedenValidationTreeFactory() {
        super(null);
    }

    @Override
    protected ValidationTree getResourceFrameValidationTree(String path) {
        ValidationTree resourceFrameValidationTree = super.getResourceFrameValidationTree(path);
        // remove validation against the Norwegian organisation registry
        resourceFrameValidationTree.removeValidationRule("AUTHORITY_ID");
        return resourceFrameValidationTree;
    }

    @Override
    protected ValidationTree getSingleFramesValidationTreeForCommonFile() {
        ValidationTree validationTree = super.getSingleFramesValidationTreeForCommonFile();
        // remove check on SiteFrame, they are part of Swedish datasets
        validationTree.removeValidationRule("SITE_FRAME_IN_COMMON_FILE");
        // allow common files that contain only a SiteFrame
        validationTree.removeValidationRule("VALIDITY_CONDITIONS_IN_COMMON_FILE_1");
        validationTree.addValidationRule(new ValidateAtLeastOne("ServiceFrame[validityConditions] | ServiceCalendarFrame[validityConditions] | SiteFrame[validityConditions]", "Neither ServiceFrame nor ServiceCalendarFrame nor SiteFrame defines ValidityConditions", "VALIDITY_CONDITIONS_IN_COMMON_FILE_SE_1"));
        return validationTree;
    }

    @Override
    protected List<ValidationRule> getCompositeFrameBaseValidationRules() {
        List<ValidationRule> compositeFrameBaseValidationRules = super.getCompositeFrameBaseValidationRules();
        // allow common files that contain only a SiteFrame
        compositeFrameBaseValidationRules.removeIf(validationRule -> validationRule.getCode().equals("SITE_FRAME_IN_COMMON_FILE"));
        return compositeFrameBaseValidationRules;
    }

}
