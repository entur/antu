package no.entur.antu.flex.validator;

import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.ValidationRule;
import org.entur.netex.validation.validator.xpath.ValidationTree;

import java.util.List;

/**
 * XPath validation tree for flexible transport timetable data.
 */
public class EnturFlexTimetableDataValidationTreeFactory extends EnturTimetableDataValidationTreeFactory {
    public EnturFlexTimetableDataValidationTreeFactory(OrganisationRepository organisationRepository) {
        super(organisationRepository);
    }

    @Override
    protected ValidationTree getSingleFramesValidationTreeForCommonFile() {
        ValidationTree validationTree = super.getSingleFramesValidationTreeForCommonFile();
        // remove check on SiteFrame, this is a valid part of flexible datasets
        validationTree.removeValidationRule("SITE_FRAME_IN_COMMON_FILE");
        return validationTree;
    }

    @Override
    protected List<ValidationRule> getCompositeFrameBaseValidationRules() {
        List<ValidationRule> compositeFrameBaseValidationRules = super.getCompositeFrameBaseValidationRules();
        // allow common files that contain  a SiteFrame
        compositeFrameBaseValidationRules.removeIf(validationRule -> validationRule.getCode().equals("SITE_FRAME_IN_COMMON_FILE"));

        return compositeFrameBaseValidationRules;
    }
}