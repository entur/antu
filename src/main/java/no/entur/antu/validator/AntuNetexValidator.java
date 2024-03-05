package no.entur.antu.validator;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.ValidationContext;

public abstract class AntuNetexValidator extends AbstractNetexValidator {

  protected AntuNetexValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  protected abstract RuleCode[] getRuleCodes();

  protected void addValidationReportEntry(
    ValidationReport validationReport,
    ValidationContext validationContext,
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
    ValidationContext validationContext,
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
