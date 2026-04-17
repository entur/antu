package no.entur.antu.validation.validator.line;

import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * Validate that OperatorRef is present on Line.
 */
public class MissingOperatorRefOnLine extends ValidateNotExist {

  public MissingOperatorRefOnLine() {
    super(
      "lines/Line[not(OperatorRef)]",
      "MISSING_OPERATOR_REF_ON_LINE",
      "OperatorRef is required on Line",
      "OperatorRef is required on Line",
      Severity.WARNING
    );
  }
}
