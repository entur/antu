package no.entur.antu.sweden.stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.AbstractXPathValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify that Stop place and Quays referenced in the dataset are defined in a SiteFrame inside the dataset.
 */
public class SwedenStopPlaceValidator extends AbstractXPathValidator {

  private static final String MESSAGE_FORMAT_UNRESOLVED_EXTERNAL_REFERENCE_TO_STOP_OR_QUAY =
    "Unresolved reference to stop place or quay";
  private static final String RULE_CODE_STOP_PLACE_REF_SE_1 =
    "STOP_PLACE_REF_SE_1";

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SwedenStopPlaceValidator.class
  );

  private final SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository;

  public SwedenStopPlaceValidator(
    SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository,
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
    this.swedenStopPlaceNetexIdRepository = swedenStopPlaceNetexIdRepository;
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    XPathValidationContext validationContext
  ) {
    LOGGER.debug(
      "Validating file {} in report {}",
      validationContext.getFileName(),
      validationReport.getValidationReportId()
    );
    validationReport.addAllValidationReportEntries(
      validate(
        validationReport.getValidationReportId(),
        validationContext.getFileName(),
        validationContext.getLocalIds(),
        validationContext.getLocalRefs()
      )
    );
    LOGGER.debug(
      "Validated file {} in report {}",
      validationContext.getFileName(),
      validationReport.getValidationReportId()
    );
  }

  protected List<ValidationReportEntry> validate(
    String reportId,
    String fileName,
    Set<IdVersion> netexFileLocalIds,
    List<IdVersion> netexRefs
  ) {
    return validateStopPlace(reportId, fileName, netexFileLocalIds, netexRefs);
  }

  private List<ValidationReportEntry> validateStopPlace(
    String reportId,
    String fileName,
    Set<IdVersion> netexFileLocalIds,
    List<IdVersion> netexRefs
  ) {
    List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
    Set<IdVersion> referencesToStopAndQuay = netexRefs
      .stream()
      .filter(idVersion ->
        idVersion.getId().contains(":StopPlace:") ||
        idVersion.getId().contains(":Quay:")
      )
      .collect(Collectors.toSet());

    // remove NSR ids
    referencesToStopAndQuay.removeIf(idVersion ->
      idVersion.getId().startsWith("NSR:")
    );

    // remove local IDs
    referencesToStopAndQuay.removeAll(netexFileLocalIds);

    if (!referencesToStopAndQuay.isEmpty()) {
      Set<String> sharedStopPlaceIds =
        swedenStopPlaceNetexIdRepository.getSharedStopPlaceAndQuayIds(reportId);

      // remove shared IDs from SiteFrame
      referencesToStopAndQuay.removeIf(idVersion ->
        sharedStopPlaceIds.contains(idVersion.getId())
      );
      if (!referencesToStopAndQuay.isEmpty()) {
        for (IdVersion id : referencesToStopAndQuay) {
          LOGGER.debug(
            "Unable to validate external reference to stop or quay {} in file {}",
            id,
            fileName
          );
          validationReportEntries.add(createValidationReportEntry(id));
        }
      }
    }

    // add the stop and quays defined in this file to the repository.
    Set<String> stopPlaceIds = netexFileLocalIds
      .stream()
      .filter(idVersion ->
        idVersion.getId().contains(":StopPlace:") ||
        idVersion.getId().contains(":Quay:")
      )
      .map(IdVersion::getId)
      .collect(Collectors.toSet());
    swedenStopPlaceNetexIdRepository.addSharedStopPlaceAndQuayIds(
      reportId,
      stopPlaceIds
    );

    return validationReportEntries;
  }

  private ValidationReportEntry createValidationReportEntry(IdVersion id) {
    DataLocation location = getIdVersionLocation(id);
    return createValidationReportEntry(
      RULE_CODE_STOP_PLACE_REF_SE_1,
      location,
      MESSAGE_FORMAT_UNRESOLVED_EXTERNAL_REFERENCE_TO_STOP_OR_QUAY
    );
  }

  @Override
  public Set<String> getRuleDescriptions() {
    return Set.of(
      createRuleDescription(
        RULE_CODE_STOP_PLACE_REF_SE_1,
        MESSAGE_FORMAT_UNRESOLVED_EXTERNAL_REFERENCE_TO_STOP_OR_QUAY
      )
    );
  }
}
