package no.entur.antu.sweden.validator;

import static org.entur.netex.validation.validator.xpath.tree.DefaultCompositeFrameTreeFactory.CODE_COMPOSITE_FRAME_1;

import java.util.HashSet;
import no.entur.antu.config.ValidationParametersConfig;
import no.entur.antu.organisation.SimpleOrganisationAliasRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityRef;
import no.entur.antu.validation.validator.xpath.rules.ValidateNSRCodespace;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.rules.ValidateAtLeastOne;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;
import org.entur.netex.validation.validator.xpath.tree.DefaultCompositeFrameTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.DefaultServiceFrameValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.DefaultSingleFramesValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.DefaultSiteFrameValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.ValidationTreeBuilder;

/**
 * XPath validation tree for timetable data from Sweden.
 * Validation rules are adapted to match swedish content.
 */
public class EnturTimetableDataSwedenValidationTreeFactory
  extends EnturTimetableDataValidationTreeFactory {

  public static final String CODE_VALIDITY_CONDITIONS_IN_COMMON_FILE_SE_1 =
    "VALIDITY_CONDITIONS_IN_COMMON_FILE_SE_1";
  public static final String CODE_COMPOSITE_FRAME_SE_1 = "COMPOSITE_FRAME_SE_1";

  public EnturTimetableDataSwedenValidationTreeFactory(
    ValidationParametersConfig validationParametersConfig
  ) {
    super(
      new SimpleOrganisationAliasRepository(new HashSet<>()),
      validationParametersConfig
    );
  }

  @Override
  public ValidationTreeBuilder builder() {
    ValidationTreeBuilder builder = super.builder();

    // accept SiteFrame, they are part of swedish datasets
    siteFrameValidationTreeBuilder()
      .removeRuleForCommonFile(
        DefaultSiteFrameValidationTreeFactory.CODE_SITE_FRAME_IN_COMMON_FILE
      )
      .removeRuleForLineFile(
        DefaultSiteFrameValidationTreeFactory.CODE_SITE_FRAME_IN_LINE_FILE
      );
    // Accept ValidBetween syntax in addition to validityConditions syntax
    singleFramesValidationTreeBuilder()
      .removeRuleForCommonFile(
        DefaultSingleFramesValidationTreeFactory.CODE_VALIDITY_CONDITIONS_IN_COMMON_FILE_1
      )
      .withRuleForCommonFile(
        new ValidateAtLeastOne(
          "ServiceFrame[validityConditions or ValidBetween] | ServiceCalendarFrame[validityConditions or ValidBetween] | SiteFrame[validityConditions or ValidBetween]",
          CODE_VALIDITY_CONDITIONS_IN_COMMON_FILE_SE_1,
          "SE/Validity condition on common frames",
          "Neither ServiceFrame nor ServiceCalendarFrame nor SiteFrame defines ValidityConditions",
          Severity.ERROR
        )
      );
    // remove validation on NSR codespace
    compositeFrameValidationTreeBuilder()
      .removeRule(ValidateNSRCodespace.CODE_NSR_CODESPACE)
      // accept ValidBetween syntax in addition to validityConditions syntax
      .removeRule(
        DefaultCompositeFrameTreeFactory.CODE_COMPOSITE_FRAME_SITE_FRAME
      )
      .removeRule(CODE_COMPOSITE_FRAME_1)
      .withRule(
        new ValidateNotExist(
          ".[not(validityConditions or ValidBetween)]",
          CODE_COMPOSITE_FRAME_SE_1,
          "SE/CompositeFrame missing ValidityCondition/ValidBetween",
          "A CompositeFrame must define a ValidityCondition valid for all data within the CompositeFrame",
          Severity.ERROR
        )
      );
    // remove validation against the Norwegian agreement registry
    serviceFrameValidationTreeBuilder()
      .removeRule(ValidateAuthorityRef.CODE_AUTHORITY_REF);
    // remove time-consuming rule
    serviceFrameValidationTreeBuilder()
      .removeRule(
        DefaultServiceFrameValidationTreeFactory.CODE_PASSENGER_STOP_ASSIGNMENT_3
      );
    return builder;
  }
}
