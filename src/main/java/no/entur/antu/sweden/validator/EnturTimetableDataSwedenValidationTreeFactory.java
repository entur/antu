package no.entur.antu.sweden.validator;

import java.util.List;
import java.util.Set;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.XPathValidationRule;
import org.entur.netex.validation.validator.xpath.rules.ValidateAtLeastOne;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * XPath validation tree for timetable data from Sweden.
 * Validation rules are adapted to match swedish content.
 */
public class EnturTimetableDataSwedenValidationTreeFactory
  extends EnturTimetableDataValidationTreeFactory {

  public EnturTimetableDataSwedenValidationTreeFactory() {
    super(
      new OrganisationRepository() {
        @Override
        public void refreshCache() {}

        @Override
        public boolean isEmpty() {
          return false;
        }

        @Override
        public Set<String> getWhitelistedAuthorityIds(String codespace) {
          return Set.of();
        }
      }
    );
  }

  @Override
  protected ValidationTree getSingleFramesValidationTreeForCommonFile() {
    ValidationTree validationTree =
      super.getSingleFramesValidationTreeForCommonFile();
    // remove check on SiteFrame, they are part of Swedish datasets
    validationTree.removeValidationRule("SITE_FRAME_IN_COMMON_FILE");
    // allow common files that contain only a SiteFrame and accept ValidBetween syntax in addition to validityConditions syntax
    validationTree.removeValidationRule("VALIDITY_CONDITIONS_IN_COMMON_FILE_1");
    validationTree.addValidationRule(
      new ValidateAtLeastOne(
        "ServiceFrame[validityConditions or ValidBetween] | ServiceCalendarFrame[validityConditions or ValidBetween] | SiteFrame[validityConditions or ValidBetween]",
        "Neither ServiceFrame nor ServiceCalendarFrame nor SiteFrame defines ValidityConditions",
        "VALIDITY_CONDITIONS_IN_COMMON_FILE_SE_1"
      )
    );
    return validationTree;
  }

  @Override
  protected List<XPathValidationRule> getCompositeFrameBaseValidationRules() {
    List<XPathValidationRule> compositeFrameBaseValidationRules =
      super.getCompositeFrameBaseValidationRules();
    // remove check on NSR codespace
    compositeFrameBaseValidationRules.removeIf(validationRule ->
      validationRule.rule().code().equals("NSR_CODESPACE")
    );
    // allow common files that contain only a SiteFrame
    compositeFrameBaseValidationRules.removeIf(validationRule ->
      validationRule.rule().code().equals("SITE_FRAME_IN_COMMON_FILE")
    );

    // accept ValidBetween syntax in addition to validityConditions syntax
    compositeFrameBaseValidationRules.removeIf(validationRule ->
      validationRule.rule().code().equals("COMPOSITE_FRAME_1")
    );
    compositeFrameBaseValidationRules.add(
      new ValidateNotExist(
        ".[not(validityConditions or ValidBetween)]",
        "A CompositeFrame must define a ValidityCondition valid for all data within the CompositeFrame",
        "COMPOSITE_FRAME_SE_1"
      )
    );

    return compositeFrameBaseValidationRules;
  }

  @Override
  protected ValidationTree getResourceFrameValidationTree(String path) {
    ValidationTree resourceFrameValidationTree =
      super.getResourceFrameValidationTree(path);
    // remove validation against the Norwegian organisation registry
    resourceFrameValidationTree.removeValidationRule("AUTHORITY_ID");
    return resourceFrameValidationTree;
  }

  @Override
  protected List<XPathValidationRule> getServiceFrameBaseValidationRules() {
    List<XPathValidationRule> serviceFrameBaseValidationRules =
      super.getServiceFrameBaseValidationRules();
    // remove time-consuming rule
    serviceFrameBaseValidationRules.removeIf(validationRule ->
      "PASSENGER_STOP_ASSIGNMENT_3".equals(validationRule.rule().code())
    );
    return serviceFrameBaseValidationRules;
  }
}
