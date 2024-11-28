package no.entur.antu.validation.validator.xpath;

import java.util.List;
import java.util.Objects;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoAlightingAtFirstStopPoint;
import no.entur.antu.validation.validator.journeypattern.stoppoint.NoBoardingAtLastStopPoint;
import no.entur.antu.validation.validator.xpath.rules.ValidateAllowedCodespaces;
import no.entur.antu.validation.validator.xpath.rules.ValidateAuthorityId;
import no.entur.antu.validation.validator.xpath.rules.ValidateNSRCodespace;
import org.entur.netex.validation.validator.xpath.DefaultValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.XPathValidationRule;

/**
 * Build the tree of XPath validation rules with Entur-specific rules.
 */
public class EnturTimetableDataValidationTreeFactory
  extends DefaultValidationTreeFactory {

  private final OrganisationRepository organisationRepository;

  public EnturTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    this.organisationRepository =
      Objects.requireNonNull(organisationRepository);
  }

  @Override
  protected ValidationTree getResourceFrameValidationTree(String path) {
    ValidationTree resourceFrameValidationTree =
      super.getResourceFrameValidationTree(path);
    resourceFrameValidationTree.addValidationRule(
      new ValidateAuthorityId(organisationRepository)
    );
    return resourceFrameValidationTree;
  }

  @Override
  protected List<XPathValidationRule> getCompositeFrameBaseValidationRules() {
    List<XPathValidationRule> validationRules =
      super.getCompositeFrameBaseValidationRules();
    validationRules.add(new ValidateNSRCodespace());
    return validationRules;
  }

  @Override
  protected ValidationTree getCommonFileValidationTree() {
    ValidationTree commonFileValidationTree =
      super.getCommonFileValidationTree();
    commonFileValidationTree.addValidationRule(new ValidateAllowedCodespaces());
    return commonFileValidationTree;
  }

  @Override
  protected ValidationTree getLineFileValidationTree() {
    ValidationTree lineFileValidationTree = super.getLineFileValidationTree();
    lineFileValidationTree.addValidationRule(new ValidateAllowedCodespaces());
    return lineFileValidationTree;
  }

  @Override
  protected ValidationTree getTimetableFrameValidationTree(String path) {
    ValidationTree timetableFrameValidationTree =
      super.getTimetableFrameValidationTree(path);
    // Disabling check of duplicate DatedServiceJourney with different versions (slow test)
    timetableFrameValidationTree.removeValidationRule(
      "DATED_SERVICE_JOURNEY_4"
    );
    return timetableFrameValidationTree;
  }

  @Override
  protected ValidationTree getServiceFrameValidationTreeForLineFile(
    String path
  ) {
    ValidationTree serviceFrameValidationTreeForLineFile =
      super.getServiceFrameValidationTreeForLineFile(path);

    // No boarding at last stop point in journey pattern
    serviceFrameValidationTreeForLineFile.addValidationRule(
      new NoBoardingAtLastStopPoint(path)
    );

    // No alighting at first stop point in journey pattern
    serviceFrameValidationTreeForLineFile.addValidationRule(
      new NoAlightingAtFirstStopPoint(path)
    );

    return serviceFrameValidationTreeForLineFile;
  }
}
