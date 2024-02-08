package no.entur.antu.validator.xpath;

import java.util.List;
import java.util.Objects;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.xpath.rules.ValidateAllowedCodespaces;
import no.entur.antu.validator.xpath.rules.ValidateAuthorityId;
import no.entur.antu.validator.xpath.rules.ValidateNSRCodespace;
import org.entur.netex.validation.validator.xpath.DefaultValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.ValidationRule;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

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
  protected List<ValidationRule> getCompositeFrameBaseValidationRules() {
    List<ValidationRule> validationRules =
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
    var lastStopWithBoardingAllowed =
      """
      for-each(
        journeyPatterns/JourneyPattern,
        function($jp) {
          sort(
            $jp/pointsInSequence/StopPointInJourneyPattern,
            (),
            function($sp) {
              ($sp/xs:integer(number(@order)))
            }
          )[last()][count(ForBoarding) = 0 or ForBoarding != 'false']
        }
      )
      """;

    serviceFrameValidationTreeForLineFile.addValidationRule(
      new ValidateNotExist(
        lastStopWithBoardingAllowed,
        "Last StopPointInJourneyPattern must not allow boarding",
        "JOURNEY_PATTERN_NO_BOARDING_ALLOWED_AT_LAST_STOP"
      )
    );

    var firstStopWithAlightingAllowed =
      """
      for-each(
        journeyPatterns/JourneyPattern,
        function($jp) {
          sort(
            $jp/pointsInSequence/StopPointInJourneyPattern,
            (),
            function($sp) {
              ($sp/xs:integer(number(@order)))
            }
          )[1][count(ForAlighting) = 0 or ForAlighting != 'false']
        }
      )
      """;

    serviceFrameValidationTreeForLineFile.addValidationRule(
      new ValidateNotExist(
        firstStopWithAlightingAllowed,
        "First StopPointInJourneyPattern must not allow alighting",
        "JOURNEY_PATTERN_NO_ALIGHTING_ALLOWED_AT_FIRST_STOP"
      )
    );

    return serviceFrameValidationTreeForLineFile;
  }
}
