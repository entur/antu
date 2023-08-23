package no.entur.antu.validator.id;

import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.validation.validator.id.IdVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.VehicleModeEnumeration;

import java.util.Set;

class ReferenceToNsrValidatorTest {


    private static final String QUAY_ID = "NSR:Quay:1";
    private static final String STOP_PLACE_ID = "NSR:StopPlace:1";
    private ReferenceToNsrValidator referenceToNsrValidator;

    @BeforeEach
    void setUpTest() {
        StopPlaceRepository stopPlaceRepository = new StopPlaceRepository() {
            @Override
            public boolean hasStopPlaceId(String stopPlaceId) {
                return STOP_PLACE_ID.equals(stopPlaceId);
            }

            @Override
            public boolean hasQuayId(String quayId) {
                return QUAY_ID.equals(quayId);
            }

            @Override
            public VehicleModeEnumeration getTransportModeForStopPlaceId(String stopPlaceId) {
                return null;
            }

            @Override
            public String getTransportSubModeForStopPlaceId(String stopPlaceId) {
                return null;
            }

            @Override
            public void refreshCache() {

            }
        };

        referenceToNsrValidator = new ReferenceToNsrValidator(stopPlaceRepository);
    }

    @Test
    void testInvalidQuay() {
        IdVersion idVersion = new IdVersion("NSR:Quay:0", null, "Quay", null, null, 0, 0);
        Set<IdVersion> idsToValidate = Set.of(idVersion);
        Set<IdVersion> validIds = referenceToNsrValidator.validateReferenceIds(idsToValidate);
        Assertions.assertNotNull(validIds);
        Assertions.assertTrue(validIds.isEmpty());
    }

    @Test
    void testValidQuay() {
        IdVersion idVersion = new IdVersion(QUAY_ID, null, "Quay", null, null, 0, 0);
        Set<IdVersion> idsToValidate = Set.of(idVersion);
        Set<IdVersion> validIds = referenceToNsrValidator.validateReferenceIds(idsToValidate);
        Assertions.assertNotNull(validIds);
        Assertions.assertTrue(validIds.contains(idVersion));
    }

    @Test
    void testValidStopPlace() {
        IdVersion idVersion = new IdVersion(STOP_PLACE_ID, null, "StopPlace", null, null, 0, 0);
        Set<IdVersion> idsToValidate = Set.of(idVersion);
        Set<IdVersion> validIds = referenceToNsrValidator.validateReferenceIds(idsToValidate);
        Assertions.assertNotNull(validIds);
        Assertions.assertTrue(validIds.contains(idVersion));
    }

}
