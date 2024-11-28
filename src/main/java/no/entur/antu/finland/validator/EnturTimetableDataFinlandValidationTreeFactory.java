package no.entur.antu.finland.validator;

import java.util.List;
import java.util.Set;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.XPathValidationRule;

/**
 * XPath validation tree for timetable data from Finland.
 * Validation rules are adapted to match finnish content.
 */
public class EnturTimetableDataFinlandValidationTreeFactory
  extends EnturTimetableDataValidationTreeFactory {

  public EnturTimetableDataFinlandValidationTreeFactory() {
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
    // allow SiteFrame
    compositeFrameBaseValidationRules.removeIf(validationRule ->
      validationRule.rule().code().equals("COMPOSITE_SITE_FRAME_IN_COMMON_FILE")
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
