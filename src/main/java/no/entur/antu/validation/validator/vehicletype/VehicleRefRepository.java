package no.entur.antu.validation.validator.vehicletype;

public interface VehicleRefRepository {
  boolean hasVehicleTypeRef(String vehicleTypeRef);
  boolean hasVehicleRef(String vehicleRef);
  /**
   * Retrieve data from the Vehicle Registry and update the cache accordingly.
   */
  void refreshCache();

  /**
   * Return true if the repository is not primed.
   */
  boolean isEmpty();
}
