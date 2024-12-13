package no.entur.antu.sweden.stop;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.XPathValidator;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify that Stop place and Quays referenced in the dataset are defined in a SiteFrame inside the dataset.
 */
public class SwedenStopPlaceValidator implements XPathValidator {

  static final ValidationRule RULE = new ValidationRule(
    "STOP_PLACE_REF_SE_1",
    "SE/Missing reference to stop place or quay",
    "Unresolved reference to stop place or quay",
    Severity.ERROR
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SwedenStopPlaceValidator.class
  );

  private final SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository;

  public SwedenStopPlaceValidator(
    SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository
  ) {
    this.swedenStopPlaceNetexIdRepository = swedenStopPlaceNetexIdRepository;
  }

  @Override
  public List<ValidationIssue> validate(
    XPathValidationContext validationContext
  ) {
    LOGGER.debug(
      "Validating file {} in report {}",
      validationContext.getFileName(),
      validationContext.getValidationReportId()
    );
    return validate(
      validationContext.getValidationReportId(),
      validationContext.getFileName(),
      validationContext.getLocalIds(),
      validationContext.getLocalRefs()
    );
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }

  protected List<ValidationIssue> validate(
    String reportId,
    String fileName,
    Set<IdVersion> netexFileLocalIds,
    List<IdVersion> netexRefs
  ) {
    return validateStopPlace(reportId, fileName, netexFileLocalIds, netexRefs);
  }

  private List<ValidationIssue> validateStopPlace(
    String reportId,
    String fileName,
    Set<IdVersion> netexFileLocalIds,
    List<IdVersion> netexRefs
  ) {
    List<ValidationIssue> validationIssues = new ArrayList<>();
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
          validationIssues.add(new ValidationIssue(RULE, id.dataLocation()));
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

    return validationIssues;
  }
}
