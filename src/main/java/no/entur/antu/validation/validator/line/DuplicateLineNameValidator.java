package no.entur.antu.validation.validator.line;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.LineInfo;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.NetexDatasetValidator;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;

public class DuplicateLineNameValidator extends NetexDatasetValidator {

  private final CommonDataRepository commonDataRepository;

  public DuplicateLineNameValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository
  ) {
    super(validationReportEntryFactory);
    this.commonDataRepository = commonDataRepository;
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    List<LineInfo> lineNames = commonDataRepository.getLineNames(
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
            .map(LineInfo::fileName)
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
