package no.entur.antu.validation.validator.line;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.SimpleLine;

public class DuplicateLineNameValidator extends AbstractDatasetValidator {

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
          "DUPLICATE_LINE_NAME",
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
      .collect(Collectors.toList());

    validationReport.addAllValidationReportEntries(duplicateEntries);
    return validationReport;
  }
}
