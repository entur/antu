package no.entur.antu.common.util;

import java.util.List;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.junit.jupiter.api.Assertions;

public class TestUtils {

  public static List<ValidationIssue> filterValidationIssuesByRule(
    List<ValidationIssue> validationIssues,
    ValidationRule rule
  ) {
    return validationIssues
      .stream()
      .filter(issue -> issue.rule() == rule)
      .toList();
  }

  public static void assertHasIssuesForRule(
    List<ValidationIssue> validationIssues,
    ValidationRule rule
  ) {
    List<ValidationIssue> validationIssuesForRule =
      filterValidationIssuesByRule(validationIssues, rule);
    Assertions.assertTrue(validationIssuesForRule.size() > 0);
  }

  public static void assertNoIssuesForRule(
    List<ValidationIssue> validationIssues,
    ValidationRule rule
  ) {
    List<ValidationIssue> validationIssuesForRule =
      filterValidationIssuesByRule(validationIssues, rule);
    Assertions.assertTrue(validationIssuesForRule.isEmpty());
  }
}
