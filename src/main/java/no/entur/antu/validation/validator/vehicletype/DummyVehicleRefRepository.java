package no.entur.antu.validation.validator.vehicletype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Used when no vehicle reference repository is configured */
public class DummyVehicleRefRepository implements VehicleRefRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DummyVehicleRefRepository.class
  );

  public DummyVehicleRefRepository() {
    LOGGER.info(
      "Vehicle reference validation is disabled - using dummy repository"
    );
  }

  @Override
  public boolean hasVehicleTypeRef(String reference) {
    return false;
  }

  @Override
  public boolean hasVehicleRef(String reference) {
    return false;
  }

  @Override
  public void refreshCache() {
    // No-op: no cache to refresh when vehicle validation is disabled
  }

  @Override
  public boolean isEmpty() {
    return true;
  }
}
