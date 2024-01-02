package no.entur.antu.sweden.validator;

import java.util.HashSet;
import java.util.Set;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;

/**
 * Mark references to line from a GroupOfLines as valid by default (this reference is not used in the data pipeline).
 */
public class LineRefOnGroupOfLinesIgnorer
  implements ExternalReferenceValidator {

  @Override
  public Set<IdVersion> validateReferenceIds(
    Set<IdVersion> externalIdsToValidate
  ) {
    Set<IdVersion> ignoredReferences = new HashSet<>(externalIdsToValidate);
    ignoredReferences.retainAll(isOfSupportedTypes(externalIdsToValidate));
    return ignoredReferences;
  }

  private Set<IdVersion> isOfSupportedTypes(Set<IdVersion> references) {
    Set<IdVersion> supportedTypes = new HashSet<>();
    for (IdVersion ref : references) {
      if (
        (
          "LineRef".equals(ref.getElementName()) &&
          ref.getParentElementNames().contains("GroupOfLines")
        )
      ) {
        supportedTypes.add(ref);
      }
    }
    return supportedTypes;
  }
}
