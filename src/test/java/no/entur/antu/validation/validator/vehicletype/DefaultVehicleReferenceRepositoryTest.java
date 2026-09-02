package no.entur.antu.validation.validator.vehicletype;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultVehicleReferenceRepositoryTest {

  private VehicleReferenceResource vehicleReferenceResource;

  @BeforeEach
  void setUp() {
    this.vehicleReferenceResource = mock(VehicleReferenceResource.class);
  }

  @Test
  void testIsEmpty() {
    HashSet<String> vehicleReferences = new HashSet<>();
    DefaultVehicleRefRepository repository = new DefaultVehicleRefRepository(
      this.vehicleReferenceResource,
      vehicleReferences
    );
    assertTrue(repository.isEmpty());
  }

  @Test
  void testHasVehicleTypeCheck() {
    HashSet<String> vehicleReferences = new HashSet<>();
    DefaultVehicleRefRepository repository = new DefaultVehicleRefRepository(
      this.vehicleReferenceResource,
      vehicleReferences
    );
    assertFalse(repository.hasVehicleTypeRef("NMR:VehicleType:1"));
    vehicleReferences.add("NMR:VehicleType:1");
    assertTrue(repository.hasVehicleTypeRef("NMR:VehicleType:1"));
  }

  @Test
  void testRefreshCache() {
    HashSet<String> initialVehicleRefs = new HashSet<>();
    initialVehicleRefs.add("NMR:VehicleType:1");
    initialVehicleRefs.add("NMR:VehicleType:2");
    DefaultVehicleRefRepository repository = new DefaultVehicleRefRepository(
      this.vehicleReferenceResource,
      initialVehicleRefs
    );

    HashSet<String> refreshedVehicleRefs = new HashSet<>();
    refreshedVehicleRefs.add("NMR:VehicleType:2");
    refreshedVehicleRefs.add("NMR:VehicleType:3");
    Mockito
      .when(vehicleReferenceResource.getVehicleAndVehicleTypesRef())
      .thenReturn(refreshedVehicleRefs);

    repository.refreshCache();
    assertFalse(repository.hasVehicleTypeRef("NMR:VehicleType:1"));
    assertTrue(repository.hasVehicleTypeRef("NMR:VehicleType:2"));
    assertTrue(repository.hasVehicleTypeRef("NMR:VehicleType:3"));
  }
}
