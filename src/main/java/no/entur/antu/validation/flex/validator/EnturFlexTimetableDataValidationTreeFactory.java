package no.entur.antu.validation.flex.validator;

import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;
import org.entur.netex.validation.validator.xpath.tree.DefaultSiteFrameValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.ValidationTreeBuilder;

/**
 * XPath validation tree for flexible transport timetable data.
 */
public class EnturFlexTimetableDataValidationTreeFactory
  extends EnturTimetableDataValidationTreeFactory {

  public static final String CODE_STOP_PLACE_IN_FLEX_COMMON_FILE =
    "STOP_PLACE_IN_FLEX_COMMON_FILE";

  public EnturFlexTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    super(organisationRepository);
  }

  @Override
  public ValidationTreeBuilder builder() {
    // accept SiteFrame in common file, they are part of flex datasets
    siteFrameValidationTreeBuilder()
      .removeRuleForCommonFile(
        DefaultSiteFrameValidationTreeFactory.CODE_SITE_FRAME_IN_COMMON_FILE
      )
      // reject non-flex stop place definitions in SiteFrame
      .withRuleForCommonFile(
        new ValidateNotExist(
          "stopPlaces",
          CODE_STOP_PLACE_IN_FLEX_COMMON_FILE,
          "Flexible Line stopPlaces not allowed",
          "StopPlaces not allowed in flexible shared files",
          Severity.ERROR
        )
      );
    return super.builder();
  }
}
