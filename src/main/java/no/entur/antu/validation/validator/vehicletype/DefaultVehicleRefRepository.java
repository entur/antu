package no.entur.antu.validation.validator.vehicletype;

import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultVehicleRefRepository implements VehicleRefRepository {

  private final VehicleReferenceResource vehicleReferenceResource;
  private final Set<String> vehicleAndVehicleTypesRefCache;

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultVehicleRefRepository.class
  );

  public DefaultVehicleRefRepository(
    VehicleReferenceResource vehicleReferenceResource,
    Set<String> vehicleAndVehicleTypesRefCache
  ) {
    this.vehicleReferenceResource = vehicleReferenceResource;
    this.vehicleAndVehicleTypesRefCache = vehicleAndVehicleTypesRefCache;
  }

  @Override
  public boolean hasVehicleTypeRef(String reference) {
    return (
      vehicleAndVehicleTypesRefCache.contains(reference) &&
      reference.contains(":VehicleType:")
    );
  }

  @Override
  public boolean hasVehicleRef(String reference) {
    return (
      vehicleAndVehicleTypesRefCache.contains(reference) &&
      reference.contains(":Vehicle:")
    );
  }

  @Override
  public void refreshCache() {
    Collection<String> vehicleAliases =
      vehicleReferenceResource.getVehicleAndVehicleTypesRef();
    vehicleAndVehicleTypesRefCache.retainAll(vehicleAliases);
    vehicleAndVehicleTypesRefCache.addAll(vehicleAliases);
    LOGGER.info(
      "Vehicle registry reference cache was refreshed with {} elements",
      vehicleAndVehicleTypesRefCache.size()
    );
  }

  @Override
  public boolean isEmpty() {
    return vehicleAndVehicleTypesRefCache.isEmpty();
  }
}
