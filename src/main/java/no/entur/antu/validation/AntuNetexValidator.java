package no.entur.antu.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.jaxb.AbstractJAXBValidator;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;

public abstract class AntuNetexValidator extends AbstractJAXBValidator {

  protected AntuNetexValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    if (validationContext.isCommonFile()) {
      validateCommonFile(validationReport, validationContext);
    }

    if (!validationContext.isCommonFile()) {
      validateLineFile(validationReport, validationContext);
    }
  }

  protected abstract RuleCode[] getRuleCodes();

  protected void validateCommonFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    // Nothing here
  }

  protected void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    // Nothing here
  }

  protected void addValidationReportEntry(
    ValidationReport validationReport,
    JAXBValidationContext validationContext,
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
    JAXBValidationContext validationContext,
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
