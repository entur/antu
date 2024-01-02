package no.entur.antu.validator.id;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceId;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;
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
    try {
      return (
        StopPlaceId.isValid(id.getId()) &&
        stopPlaceRepository.hasStopPlaceId(new StopPlaceId(id.getId()))
      );
    } catch (AntuException ex) {
      LOGGER.error(ex.getMessage());
      return false;
    }
  }

  private boolean isValidQuayReference(IdVersion id) {
    try {
      return (
        QuayId.isValid(id.getId()) &&
        stopPlaceRepository.hasQuayId(new QuayId(id.getId()))
      );
    } catch (AntuException ex) {
      LOGGER.error(ex.getMessage());
      return false;
    }
  }
}
