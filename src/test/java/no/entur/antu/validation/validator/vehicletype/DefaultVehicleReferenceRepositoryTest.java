package no.entur.antu.validation.validator.vehicletype;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
  void testHasVehicleRefCheck() {
    HashSet<String> vehicleReferences = new HashSet<>();
    DefaultVehicleRefRepository repository = new DefaultVehicleRefRepository(
      this.vehicleReferenceResource,
      vehicleReferences
    );

    // Test when reference is not in cache - should return false
    assertFalse(repository.hasVehicleRef("NMR:Vehicle:1"));

    // Add a valid vehicle reference to cache
    vehicleReferences.add("NMR:Vehicle:1");
    assertTrue(repository.hasVehicleRef("NMR:Vehicle:1"));

    // Test that VehicleType references are rejected even if in cache
    vehicleReferences.add("NMR:VehicleType:2");
    assertFalse(repository.hasVehicleRef("NMR:VehicleType:2"));

    // Test that reference must contain ":Vehicle:" substring
    vehicleReferences.add("NMR:SomethingElse:3");
    assertFalse(repository.hasVehicleRef("NMR:SomethingElse:3"));
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
    when(vehicleReferenceResource.getVehicleAndVehicleTypesRef())
      .thenReturn(refreshedVehicleRefs);

    repository.refreshCache();
    assertFalse(repository.hasVehicleTypeRef("NMR:VehicleType:1"));
    assertTrue(repository.hasVehicleTypeRef("NMR:VehicleType:2"));
    assertTrue(repository.hasVehicleTypeRef("NMR:VehicleType:3"));
  }
}
