package no.entur.antu.validation.validator.vehicletype;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.IdVersion;

public class VehicleTypeReferenceIgnorer implements ExternalReferenceValidator {

  @Override
  public Set<IdVersion> validateReferenceIds(
    Set<IdVersion> externalIdsToValidate
  ) {
    Objects.requireNonNull(externalIdsToValidate);
    return externalIdsToValidate
      .stream()
      .filter(VehicleTypeReferenceIgnorer::isIgnorableVehicleReference)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static boolean isIgnorableVehicleReference(IdVersion ref) {
    return (
      (
        "VehicleTypeRef".equals(ref.getElementName()) &&
        ref.getId().contains("VehicleType")
      ) ||
      (
        "VehicleRef".equals(ref.getElementName()) &&
        ref.getId().contains("Vehicle")
      )
    );
  }
}
