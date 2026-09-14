package no.entur.antu.netex.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;

/**
 * Assertions scoped to one ValidationRule, for validators that report several rules and where a
 * test cares about exactly one of them.
 */
public final class ValidationIssueAssertions {

  private ValidationIssueAssertions() {}

  public static List<ValidationIssue> issuesForRule(
    List<ValidationIssue> validationIssues,
    ValidationRule rule
  ) {
    return validationIssues
      .stream()
      .filter(validationIssue -> validationIssue.rule() == rule)
      .toList();
  }

  public static void assertHasIssuesForRule(
    List<ValidationIssue> validationIssues,
    ValidationRule rule
  ) {
    assertFalse(
      issuesForRule(validationIssues, rule).isEmpty(),
      "expected at least one issue for rule " + rule.name()
    );
  }

  public static void assertNoIssuesForRule(
    List<ValidationIssue> validationIssues,
    ValidationRule rule
  ) {
    assertTrue(
      issuesForRule(validationIssues, rule).isEmpty(),
      "expected no issues for rule " + rule.name()
    );
  }
}
