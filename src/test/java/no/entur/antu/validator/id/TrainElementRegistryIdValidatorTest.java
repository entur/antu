package no.entur.antu.validator.id;

import org.entur.netex.validation.validator.id.IdVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

class TrainElementRegistryIdValidatorTest {


    private static final String VALID_TRAIN_ELEMENT_ID = TrainElementRegistryIdValidator.TRAIN_ELEMENT_CODESPACE + ":" + TrainElementRegistryIdValidator.TRAIN_ELEMENT_TYPE + ":1";
    private static final String INVALID_TRAIN_ELEMENT_ID_WRONG_CODESPACE = "XXX:" + TrainElementRegistryIdValidator.TRAIN_ELEMENT_TYPE + ":1";
    private static final String INVALID_TRAIN_ELEMENT_ID_WRONG_ELEMENT_TYPE = TrainElementRegistryIdValidator.TRAIN_ELEMENT_CODESPACE + ":XX:1";
    private TrainElementRegistryIdValidator trainElementRegistryIdValidator;

    @BeforeEach
    void setUpTest() {
        trainElementRegistryIdValidator = new TrainElementRegistryIdValidator();
    }

    @Test
    void testInvalidTrainElementWrongCodespace() {
        IdVersion idVersion = new IdVersion(INVALID_TRAIN_ELEMENT_ID_WRONG_CODESPACE, null, TrainElementRegistryIdValidator.TRAIN_ELEMENT_TYPE, null, null, 0, 0);
        Set<IdVersion> idsToValidate = Set.of(idVersion);
        Set<IdVersion> validIds = trainElementRegistryIdValidator.validateReferenceIds(idsToValidate);
        Assertions.assertNotNull(validIds);
        Assertions.assertTrue(validIds.isEmpty());
    }

    @Test
    void testInvalidTrainElementWrongElementType() {
        IdVersion idVersion = new IdVersion(INVALID_TRAIN_ELEMENT_ID_WRONG_ELEMENT_TYPE, null, TrainElementRegistryIdValidator.TRAIN_ELEMENT_TYPE, null, null, 0, 0);
        Set<IdVersion> idsToValidate = Set.of(idVersion);
        Set<IdVersion> validIds = trainElementRegistryIdValidator.validateReferenceIds(idsToValidate);
        Assertions.assertNotNull(validIds);
        Assertions.assertTrue(validIds.isEmpty());
    }

    @Test
    void testInvalidTrainElement() {
        IdVersion idVersion = new IdVersion("XXX:YY:ZZ", null, "EntityName", null, null, 0, 0);
        Set<IdVersion> idsToValidate = Set.of(idVersion);
        Set<IdVersion> validIds = trainElementRegistryIdValidator.validateReferenceIds(idsToValidate);
        Assertions.assertNotNull(validIds);
        Assertions.assertTrue(validIds.isEmpty());
    }

    @Test
    void testValidTrainElement() {
        TrainElementRegistryIdValidator trainElementRegistryIdValidator = new TrainElementRegistryIdValidator();
        IdVersion idVersion = new IdVersion(VALID_TRAIN_ELEMENT_ID, null, TrainElementRegistryIdValidator.TRAIN_ELEMENT_TYPE, null, null, 0, 0);
        Set<IdVersion> idsToValidate = Set.of(idVersion);
        Set<IdVersion> validIds = trainElementRegistryIdValidator.validateReferenceIds(idsToValidate);
        Assertions.assertNotNull(validIds);
        Assertions.assertTrue(validIds.contains(idVersion));
    }

}
