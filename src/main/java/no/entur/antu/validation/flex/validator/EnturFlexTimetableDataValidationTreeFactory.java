package no.entur.antu.validation.flex.validator;

import java.util.List;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.XPathValidationRule;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * XPath validation tree for flexible transport timetable data.
 */
public class EnturFlexTimetableDataValidationTreeFactory
  extends EnturTimetableDataValidationTreeFactory {

  public EnturFlexTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    super(organisationRepository);
  }

  @Override
  protected ValidationTree getSingleFramesValidationTreeForCommonFile() {
    ValidationTree validationTree =
      super.getSingleFramesValidationTreeForCommonFile();
    // remove check on SiteFrame, this is a valid part of flexible datasets
    validationTree.removeValidationRule("SITE_FRAME_IN_COMMON_FILE");
    return validationTree;
  }

  @Override
  protected List<XPathValidationRule> getCompositeFrameBaseValidationRules() {
    List<XPathValidationRule> compositeFrameBaseValidationRules =
      super.getCompositeFrameBaseValidationRules();
    // allow common files that contain a SiteFrame
    compositeFrameBaseValidationRules.removeIf(validationRule ->
      validationRule.rule().code().equals("COMPOSITE_SITE_FRAME_IN_COMMON_FILE")
    );

    return compositeFrameBaseValidationRules;
  }

  @Override
  protected ValidationTree getCompositeFrameValidationTreeForCommonFile() {
    ValidationTree validationTree =
      super.getCompositeFrameValidationTreeForCommonFile();

    validationTree.addSubTree(
      getSiteFrameValidationTreeForCommonFile("frames/SiteFrame")
    );

    return validationTree;
  }

  @Override
  protected ValidationTree getSiteFrameValidationTreeForCommonFile(
    String path
  ) {
    ValidationTree siteFrameValidationTree = new ValidationTree(
      "Site frame in common file",
      path
    );

    siteFrameValidationTree.addValidationRule(
      new ValidateNotExist(
        "stopPlaces",
        "stopPlaces not allowed in flexible shared files",
        "SITE_FRAME_IN_COMMON_FILE_1"
      )
    );

    return siteFrameValidationTree;
  }
}
