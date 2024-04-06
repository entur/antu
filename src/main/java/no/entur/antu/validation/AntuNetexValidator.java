package no.entur.antu.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.AbstractNetexValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.ValidationContext;

public abstract class AntuNetexValidator extends AbstractNetexValidator {

  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  protected AntuNetexValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(validationReportEntryFactory);
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
  }

  protected abstract RuleCode[] getRuleCodes();

  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    if (validationContext.isCommonFile()) {
      validateCommonFile(validationReport, validationContext);
    }

    if (!validationContext.isCommonFile()) {
      validateLineFile(validationReport, validationContext);
    }
  }

  protected NetexEntitiesIndex getNetexEntitiesIndex(
    ValidationContext validationContext
  ) {
    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      return validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();
    } else {
      throw new AntuException("Netex entities index not available in context");
    }
  }

  protected abstract void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  );

  protected abstract void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  );

  protected AntuNetexData createAntuNetexData(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    return new AntuNetexData(
      validationReport.getValidationReportId(),
      getNetexEntitiesIndex(validationContext),
      commonDataRepository,
      stopPlaceRepository
    );
  }

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
