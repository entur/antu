package no.entur.antu.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.AbstractXPathValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;

public abstract class AntuNetexValidator extends AbstractXPathValidator {

  protected AntuNetexValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  protected abstract RuleCode[] getRuleCodes();

  public void validate(
    ValidationReport validationReport,
    XPathValidationContext validationContext
  ) {
    AntuNetexData antuNetexData = getAntuNetexData(validationContext);

    if (validationContext.isCommonFile()) {
      validateCommonFile(validationReport, validationContext, antuNetexData);
    }

    if (!validationContext.isCommonFile()) {
      validateLineFile(validationReport, validationContext, antuNetexData);
    }
  }

  private AntuNetexData getAntuNetexData(
    XPathValidationContext validationContext
  ) {
    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      return validationContextWithNetexEntitiesIndex.getAntuNetexData();
    } else {
      throw new AntuException("Netex data not available in context");
    }
  }

  protected void validateCommonFile(
    ValidationReport validationReport,
    XPathValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // Nothing here
  }

  protected void validateLineFile(
    ValidationReport validationReport,
    XPathValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // Nothing here
  }

  protected void addValidationReportEntry(
    ValidationReport validationReport,
    XPathValidationContext validationContext,
    ValidationError validationError
  ) {
    ValidationReportEntry validationReportEntry = createValidationReportEntry(
      validationError.getRuleCode(),
      findDataLocation(validationContext, validationError.getEntityId()),
      validationError.validationReportEntryMessage()
    );

    validationReport.addValidationReportEntry(validationReportEntry);
  }

  private static DataLocation findDataLocation(
    XPathValidationContext validationContext,
    String entityId
  ) {
    String fileName = validationContext.getFileName();

    IdVersion idVersion = validationContext.getLocalIdsMap().get(entityId);

    return idVersion != null
      ? new DataLocation(
        idVersion.getId(),
        validationContext.getFileName(),
        idVersion.getLineNumber(),
        idVersion.getColumnNumber()
      )
      : new DataLocation(entityId, fileName, 0, 0);
  }

  @Override
  public final Set<String> getRuleDescriptions() {
    return Arrays
      .stream(getRuleCodes())
      .map(ruleCode ->
        createRuleDescription(ruleCode.toString(), ruleCode.getErrorMessage())
      )
      .collect(Collectors.toSet());
  }
}
