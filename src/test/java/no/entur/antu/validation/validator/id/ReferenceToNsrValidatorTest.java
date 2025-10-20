package no.entur.antu.validation.validator.id;

import java.util.Set;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReferenceToNsrValidatorTest {

  private static final String QUAY_ID = "NSR:Quay:1";
  private static final String STOP_PLACE_ID = "NSR:StopPlace:1";
  private ReferenceToNsrValidator referenceToNsrValidator;

  @BeforeEach
  void setUpTest() {
    StopPlaceRepository stopPlaceRepository = new StopPlaceRepository() {
      @Override
      public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
        return STOP_PLACE_ID.equals(stopPlaceId.id());
      }

      @Override
      public boolean hasQuayId(QuayId quayId) {
        return QUAY_ID.equals(quayId.id());
      }

      @Override
      public TransportModeAndSubMode getTransportModesForQuayId(QuayId quayId) {
        return null;
      }

      @Override
      public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
        return null;
      }

      @Override
      public String getStopPlaceNameForQuayId(QuayId quayId) {
        return null;
      }
    };

    referenceToNsrValidator = new ReferenceToNsrValidator(stopPlaceRepository);
  }

  @Test
  void testInvalidQuay() {
    IdVersion idVersion = new IdVersion(
      "NSR:Quay:0",
      null,
      "Quay",
      null,
      null,
      0,
      0
    );
    Set<IdVersion> idsToValidate = Set.of(idVersion);
    Set<IdVersion> validIds = referenceToNsrValidator.validateReferenceIds(
      idsToValidate
    );
    Assertions.assertNotNull(validIds);
    Assertions.assertTrue(validIds.isEmpty());
  }

  @Test
  void testValidQuay() {
    IdVersion idVersion = new IdVersion(
      QUAY_ID,
      null,
      "Quay",
      null,
      null,
      0,
      0
    );
    Set<IdVersion> idsToValidate = Set.of(idVersion);
    Set<IdVersion> validIds = referenceToNsrValidator.validateReferenceIds(
      idsToValidate
    );
    Assertions.assertNotNull(validIds);
    Assertions.assertTrue(validIds.contains(idVersion));
  }

  @Test
  void testValidStopPlace() {
    IdVersion idVersion = new IdVersion(
      STOP_PLACE_ID,
      null,
      "StopPlace",
      null,
      null,
      0,
      0
    );
    Set<IdVersion> idsToValidate = Set.of(idVersion);
    Set<IdVersion> validIds = referenceToNsrValidator.validateReferenceIds(
      idsToValidate
    );
    Assertions.assertNotNull(validIds);
    Assertions.assertTrue(validIds.contains(idVersion));
  }
}
