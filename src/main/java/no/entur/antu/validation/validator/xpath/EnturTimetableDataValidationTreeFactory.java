package no.entur.antu.validation.validator.xpath;

import java.util.Objects;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoAlightingAtFirstStopPoint;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoBoardingAtLastStopPoint;
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

  public EnturTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    this.organisationRepository =
      Objects.requireNonNull(organisationRepository);
  }

  @Override
  public ValidationTreeBuilder builder() {
    // Validation against the Norwegian organisation register
    resourceFrameValidationTreeBuilder()
      .withRule(new ValidateAuthorityId(organisationRepository));
    // Validation against Norwegian codespaces
    compositeFrameValidationTreeBuilder().withRule(new ValidateNSRCodespace());
    rootValidationTreeBuilder().withRule(new ValidateAllowedCodespaces());
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
