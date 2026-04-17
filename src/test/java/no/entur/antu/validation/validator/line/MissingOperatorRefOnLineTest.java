package no.entur.antu.validation.validator.line;

import java.util.List;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MissingOperatorRefOnLineTest {

  private static final String LINE_WITH_OPERATOR_REF =
    """
    <ServiceFrame xmlns="http://www.netex.org.uk/netex" version="1" id="TST:ServiceFrame:1">
      <lines>
        <Line version="1" id="TST:Line:1">
          <Name>Test Line</Name>
          <OperatorRef ref="TST:Operator:1"/>
        </Line>
      </lines>
    </ServiceFrame>
    """;

  private static final String LINE_WITHOUT_OPERATOR_REF =
    """
    <ServiceFrame xmlns="http://www.netex.org.uk/netex" version="1" id="TST:ServiceFrame:1">
      <lines>
        <Line version="1" id="TST:Line:1">
          <Name>Test Line</Name>
        </Line>
      </lines>
    </ServiceFrame>
    """;

  @Test
  void lineWithOperatorRefShouldNotProduceValidationIssue() {
    List<ValidationIssue> issues = runTestWith(LINE_WITH_OPERATOR_REF);
    Assertions.assertTrue(issues.isEmpty());
  }

  @Test
  void lineWithoutOperatorRefShouldProduceValidationIssue() {
    List<ValidationIssue> issues = runTestWith(LINE_WITHOUT_OPERATOR_REF);
    Assertions.assertFalse(issues.isEmpty());
    Assertions.assertEquals(1, issues.size());
  }

  private List<ValidationIssue> runTestWith(String netexFragment) {
    XPathRuleValidationContext context = TestValidationContextBuilder
      .ofNetexFragment(netexFragment)
      .withCodespace("TST")
      .build();
    return new MissingOperatorRefOnLine().validate(context);
  }
}
