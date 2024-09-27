package no.entur.antu.validation.validator.id;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that NeTEX references point to a valid stop or quay in the National Stop Place Registry.
 */
public class ReferenceToNsrValidator implements ExternalReferenceValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ReferenceToNsrValidator.class
  );

  private final StopPlaceRepository stopPlaceRepository;

  public ReferenceToNsrValidator(StopPlaceRepository stopPlaceRepository) {
    this.stopPlaceRepository = Objects.requireNonNull(stopPlaceRepository);
  }

  @Override
  public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIds) {
    Objects.requireNonNull(externalIds);
    Set<IdVersion> validIds = new HashSet<>();

    for (IdVersion id : externalIds) {
      if (isValidQuayReference(id) || isValidStopPlaceReference(id)) {
        validIds.add(id);
      }
    }

    return validIds;
  }

  private boolean isValidStopPlaceReference(IdVersion id) {
    return (
      StopPlaceId.isValid(id.getId()) &&
      stopPlaceRepository.hasStopPlaceId(new StopPlaceId(id.getId()))
    );
  }

  private boolean isValidQuayReference(IdVersion id) {
    return (
      QuayId.isValid(id.getId()) &&
      stopPlaceRepository.hasQuayId(new QuayId(id.getId()))
    );
  }
}
