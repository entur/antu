package no.entur.antu.validation.validator.interchange.mandatoryfields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;

/**
 * Validator for mandatory fields in interchange.
 * For validation of whether referred ServiceJourneys exist, see InterchangeServiceJourneyReferencesExistValidator.
 * Chouette reference (partially implemented here):
 *  3-Interchange-1,
 *  3-Interchange-2,
 *  3-Interchange-3,
 *  3-Interchange-4
 */
public class MandatoryFieldsValidator implements JAXBValidator {

  static final ValidationRule RULE_MISSING_FROM_STOP_POINT_IN_INTERCHANGE =
    new ValidationRule(
      "MISSING_FROM_STOP_POINT_IN_INTERCHANGE",
      "Mandatory field FromPointRef is missing in ServiceJourneyInterchange",
      Severity.ERROR
    );

  static final ValidationRule RULE_MISSING_TO_STOP_POINT_IN_INTERCHANGE =
    new ValidationRule(
      "MISSING_TO_STOP_POINT_IN_INTERCHANGE",
      "Mandatory field ToPointRef is missing in ServiceJourneyInterchange",
      Severity.ERROR
    );
  static final ValidationRule RULE_MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE =
    new ValidationRule(
      "MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE",
      "Mandatory field FromJourneyRef is missing in ServiceJourneyInterchange",
      Severity.ERROR
    );

  static final ValidationRule RULE_MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE =
    new ValidationRule(
      "MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE",
      "Mandatory field ToJourneyRef is missing in ServiceJourneyInterchange",
      Severity.ERROR
    );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .serviceJourneyInterchanges()
      .stream()
      .map(MandatoryFieldsContext::of)
      .map(mandatoryFieldsContext ->
        validateMandatoryFields(validationContext, mandatoryFieldsContext)
      )
      .flatMap(Collection::stream)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(
      RULE_MISSING_FROM_STOP_POINT_IN_INTERCHANGE,
      RULE_MISSING_TO_STOP_POINT_IN_INTERCHANGE,
      RULE_MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE,
      RULE_MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE
    );
  }

  private List<ValidationIssue> validateMandatoryFields(
    JAXBValidationContext validationContext,
    MandatoryFieldsContext context
  ) {
    List<ValidationIssue> issues = new ArrayList<>();

    // 3-Interchange-1
    if (context.fromPointRef() == null) {
      issues.add(
        new ValidationIssue(
          RULE_MISSING_FROM_STOP_POINT_IN_INTERCHANGE,
          validationContext.dataLocation(context.interchangeId())
        )
      );
    }

    // 3-Interchange-2
    if (context.toPointRef() == null) {
      issues.add(
        new ValidationIssue(
          RULE_MISSING_TO_STOP_POINT_IN_INTERCHANGE,
          validationContext.dataLocation(context.interchangeId())
        )
      );
    }

    // 3-Interchange-3
    if (context.fromJourneyRef() == null) {
      issues.add(
        new ValidationIssue(
          RULE_MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE,
          validationContext.dataLocation(context.interchangeId())
        )
      );
    }

    // 3-Interchange-4
    if (context.toJourneyRef() == null) {
      issues.add(
        new ValidationIssue(
          RULE_MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE,
          validationContext.dataLocation(context.interchangeId())
        )
      );
    }

    return issues;
  }
}
