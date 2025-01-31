package no.entur.antu.validation.validator.xpath;

import java.util.Objects;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.organisation.OrganisationV3Repository;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoAlightingAtFirstStopPoint;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoBoardingAtLastStopPoint;
import no.entur.antu.validation.validator.xpath.rules.ValidateReferenceToOrgV3;
import no.entur.antu.validation.validator.xpath.rules.ValidateAllowedCodespaces;
import no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityId;
import no.entur.antu.validation.validator.xpath.rules.ValidateNSRCodespace;
import org.entur.netex.validation.validator.xpath.tree.DefaultTimetableFrameValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.PublicationDeliveryValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.ValidationTreeBuilder;

/**
 * Build the tree of XPath validation rules with Entur-specific rules.
 */
public class EnturTimetableDataValidationTreeFactory
  extends PublicationDeliveryValidationTreeFactory {

  private final OrganisationRepository organisationRepository;
  private final OrganisationV3Repository defaultOrganisationV3Repository;

  public EnturTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository,
    OrganisationV3Repository defaultOrganisationV3Repository
  ) {
    this.organisationRepository =
      Objects.requireNonNull(organisationRepository);
    this.defaultOrganisationV3Repository =
      Objects.requireNonNull(defaultOrganisationV3Repository);
  }

  @Override
  public ValidationTreeBuilder builder() {
    // Validation against the Norwegian organisation register
    resourceFrameValidationTreeBuilder()
      .withRule(new ValidateAuthorityId(organisationRepository));
    // Validation against Norwegian codespaces
    compositeFrameValidationTreeBuilder().withRule(new ValidateNSRCodespace());
    rootValidationTreeBuilder().withRule(new ValidateAllowedCodespaces());
    rootValidationTreeBuilder()
      .withRule(new ValidateReferenceToOrgV3(defaultOrganisationV3Repository));
    // Disabling check of duplicate DatedServiceJourney with different versions (slow test)
    timetableFrameValidationTreeBuilder()
      .removeRule(
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
