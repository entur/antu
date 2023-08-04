package no.entur.antu.validation.validator.id;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;

/**
 * Validate that TrainElement objects refer to the PEN codespace (Entur-plass).
 */
public class TrainElementRegistryIdValidator
  implements ExternalReferenceValidator {

  static final String TRAIN_ELEMENT_CODESPACE = "PEN";

  static final String TRAIN_ELEMENT_TYPE = "TrainElement";

  @Override
  public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIds) {
    Objects.requireNonNull(externalIds);
    return externalIds
      .stream()
      .filter(TrainElementRegistryIdValidator::isValidTrainElementId)
      .collect(Collectors.toSet());
  }

  private static boolean isValidTrainElementId(IdVersion idVersion) {
    String[] idParts = idVersion.getId().split(":");
    return (
      idParts.length == 3 &&
      TRAIN_ELEMENT_CODESPACE.equals(idParts[0]) &&
      TRAIN_ELEMENT_TYPE.equals(idParts[1])
    );
  }
}
