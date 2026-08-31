package no.entur.antu.validation.validator.id;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import no.entur.antu.validation.validator.vehicletype.VehicleRefRepository;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that NeTEX references point to a valid stop or quay in the National Stop Place Registry.
 */
public class ReferenceToVehicleRegistryValidator
  implements ExternalReferenceValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ReferenceToVehicleRegistryValidator.class
  );

  private final VehicleRefRepository vehicleRefRepository;

  public ReferenceToVehicleRegistryValidator(
    VehicleRefRepository vehicleRefRepository
  ) {
    this.vehicleRefRepository = Objects.requireNonNull(vehicleRefRepository);
  }

  @Override
  public Set<IdVersion> validateReferenceIds(Set<IdVersion> externalIds) {
    Objects.requireNonNull(externalIds);
    Set<IdVersion> validIds = new HashSet<>();

    for (IdVersion id : externalIds) {
      if (isValidVehicleTypeReference(id) || isValidVehicleReference(id)) {
        validIds.add(id);
      }
    }

    return validIds;
  }

  private boolean isValidVehicleTypeReference(IdVersion id) {
    return vehicleRefRepository.hasVehicleTypeRef(id.getId());
  }

  private boolean isValidVehicleReference(IdVersion id) {
    return vehicleRefRepository.hasVehicleRef(id.getId());
  }
}
