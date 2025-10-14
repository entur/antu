package no.entur.antu.validation.validator.xpath;

import no.entur.antu.config.ValidationParametersConfig;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoAlightingAtFirstStopPoint;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoBoardingAtLastStopPoint;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import no.entur.antu.validation.validator.xpath.rules.ValidateAllowedCodespaces;
import no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityRef;
import no.entur.antu.validation.validator.xpath.rules.ValidateNSRCodespace;
import org.entur.netex.validation.validator.xpath.tree.DefaultTimetableFrameValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.PublicationDeliveryValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.ValidationTreeBuilder;

/**
 * Build the tree of XPath validation rules with Entur-specific rules.
 */
public class EnturTimetableDataValidationTreeFactory
  extends PublicationDeliveryValidationTreeFactory {

  private final OrganisationAliasRepository organisationAliasRepository;
  private final ValidationParametersConfig validationParametersConfig;

  public EnturTimetableDataValidationTreeFactory(
    OrganisationAliasRepository organisationAliasRepository,
    ValidationParametersConfig validationParametersConfig
  ) {
    this.organisationAliasRepository = organisationAliasRepository;
    this.validationParametersConfig = validationParametersConfig;
  }

  @Override
  public ValidationTreeBuilder builder() {
    // Validation against the Norwegian organisation register
    serviceFrameValidationTreeBuilder()
      .withRule(new ValidateAuthorityRef(organisationAliasRepository));
    // Validation against Norwegian codespaces
    compositeFrameValidationTreeBuilder().withRule(new ValidateNSRCodespace());

    ValidateAllowedCodespaces validateAllowedCodespaces = new ValidateAllowedCodespaces(
        validationParametersConfig.getAdditionalAllowedCodespaces()
    );
    rootValidationTreeBuilder().withRule(validateAllowedCodespaces);

    // Disabling check of duplicate ServiceJourney with different versions (slow test)
    timetableFrameValidationTreeBuilder()
      .removeRuleForLineFile(
        DefaultTimetableFrameValidationTreeFactory.CODE_SERVICE_JOURNEY_16
      );

    // Disabling check of duplicate DatedServiceJourney with different versions (slow test)
    timetableFrameValidationTreeBuilder()
      .removeRuleForLineFile(
        DefaultTimetableFrameValidationTreeFactory.CODE_DATED_SERVICE_JOURNEY_4
      );

    // No boarding at last stop point in journey pattern
    serviceFrameValidationTreeBuilder()
      .withRuleForLineFile(new NoBoardingAtLastStopPoint());
    // No alighting at first stop point in journey pattern
    serviceFrameValidationTreeBuilder()
      .withRuleForLineFile(new NoAlightingAtFirstStopPoint());
    return super.builder();
  }
}
