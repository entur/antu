package no.entur.antu.finland.validator;

import no.entur.antu.agreement.SimpleAgreementRepository;
import no.entur.antu.organisation.SimpleOrganisationRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityRef;
import no.entur.antu.validation.validator.xpath.rules.ValidateNSRCodespace;
import org.entur.netex.validation.validator.xpath.tree.DefaultCompositeFrameTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.DefaultServiceFrameValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.DefaultSiteFrameValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.ValidationTreeBuilder;

/**
 * XPath validation tree for timetable data from Finland.
 * Validation rules are adapted to match finnish content.
 */
public class EnturTimetableDataFinlandValidationTreeFactory
  extends EnturTimetableDataValidationTreeFactory {

  public EnturTimetableDataFinlandValidationTreeFactory() {
    super(
      new SimpleOrganisationRepository(),
      new SimpleAgreementRepository()
    );
  }

  @Override
  public ValidationTreeBuilder builder() {
    ValidationTreeBuilder builder = super.builder();

    // accept SiteFrame, they are part of Finnish datasets
    siteFrameValidationTreeBuilder()
      .removeRuleForCommonFile(
        DefaultSiteFrameValidationTreeFactory.CODE_SITE_FRAME_IN_COMMON_FILE
      )
      .removeRuleForLineFile(
        DefaultSiteFrameValidationTreeFactory.CODE_SITE_FRAME_IN_LINE_FILE
      );
    // remove validation on NSR codespace
    compositeFrameValidationTreeBuilder()
      .removeRule(ValidateNSRCodespace.CODE_NSR_CODESPACE)
      .removeRule(
        DefaultCompositeFrameTreeFactory.CODE_COMPOSITE_FRAME_SITE_FRAME
      );
    // remove validation against the Norwegian organisation registry
    resourceFrameValidationTreeBuilder()
      .removeRule(ValidateAuthorityRef.CODE_AUTHORITY_REF);
    serviceFrameValidationTreeBuilder()
      .removeRule(
        DefaultServiceFrameValidationTreeFactory.CODE_PASSENGER_STOP_ASSIGNMENT_3
      );
    return builder;
  }
}
