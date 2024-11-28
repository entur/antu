package no.entur.antu.validation.validator.line;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.SimpleLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DuplicateLineNameValidator extends AbstractDatasetValidator {

  static final ValidationRule RULE = new ValidationRule(
    "DUPLICATE_LINE_NAME",
    "Duplicate line names found",
    Severity.WARNING
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DuplicateLineNameValidator.class
  );

  private final NetexDataRepository netexDataRepository;

  public DuplicateLineNameValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    LOGGER.info("Validating duplicate line names.");

    List<SimpleLine> lineNames = netexDataRepository.lineNames(
      validationReport.getValidationReportId()
    );

    List<ValidationReportEntry> duplicateEntries = lineNames
      .stream()
      .collect(Collectors.groupingBy(Function.identity(), Collectors.toList()))
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue().size() > 1)
      .map(entry ->
        createValidationReportEntry(
          new ValidationIssue(
            RULE,
            new DataLocation(
              entry.getKey().lineId(),
              entry.getKey().fileName(),
              0,
              0
            ),
            entry.getKey().lineName() +
            " is used in line files " +
            entry
              .getValue()
              .stream()
              .map(SimpleLine::fileName)
              .filter(filename -> !filename.equals(entry.getKey().fileName()))
              .map(filename -> "'" + filename + "'")
              .collect(Collectors.joining(", "))
          )
        )
      )
      .toList();

    validationReport.addAllValidationReportEntries(duplicateEntries);
    return validationReport;
  }
}
