package no.entur.antu.config.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import no.entur.antu.job.ValidationContext;
import org.junit.jupiter.api.Test;
import org.redisson.client.handler.State;
import org.redisson.codec.Kryo5Codec;

/**
 * Couples the shape of {@link ValidationState} to the name of the cache it is stored in.
 *
 * <p>The cache uses a bare {@code Kryo5Codec}. Its {@code FieldSerializer} writes fields positionally with
 * no schema header, so the class is a wire format shared by two antu versions during a rollout, and it
 * cannot evolve in place. Growing it from one field to three took the stalled-validation sweep down for
 * hours in dev: {@code allValidationStates()} scans the whole hash, so a single entry left by the previous
 * version threw and aborted every sweep, silently, while the pipeline itself looked fine.
 *
 * <p>Nothing about the language stops that happening again, so this test does. If you change the fields of
 * {@code ValidationState}, or of the {@code ValidationContext} nested inside it, this fails until you also
 * move the cache to a new name.
 */
class ValidationStateSerializationTest {

  /**
   * Sorted by name because that is how Kryo orders fields, so a pure reordering of the declarations is not
   * a wire change and should not fail here. Adding, removing, renaming or retyping one is.
   */
  private static List<String> fieldSignature(Class<?> type) {
    return Arrays
      .stream(type.getDeclaredFields())
      .filter(field -> !field.isSynthetic())
      .filter(field -> !Modifier.isStatic(field.getModifiers()))
      .map(field -> field.getName() + ":" + field.getType().getSimpleName())
      .sorted()
      .toList();
  }

  @Test
  void changingTheFieldsRequiresANewCacheName() {
    assertEquals(
      List.of(
        "context:ValidationContext",
        "hasErrorInCommonFile:boolean",
        "lastProgressAt:Instant"
      ),
      fieldSignature(ValidationState.class),
      """
      ValidationState is a Kryo wire format shared across a rollout, not just an internal class.
      If you changed its fields, bump the suffix on CacheConfig.VALIDATION_STATE_CACHE and update
      this expectation in the same commit. Otherwise the new version throws on every entry the old
      one left behind, and the stalled-validation sweep dies without saying so."""
    );

    assertEquals(
      "validationProgressCache.v2",
      CacheConfig.VALIDATION_STATE_CACHE,
      "the cache name has to change whenever the serialized shape above does"
    );
  }

  /**
   * {@code ValidationContext} is serialized inside {@code ValidationState}, so its shape is part of the
   * same wire format and carries the same obligation.
   */
  @Test
  void theNestedContextIsPartOfTheSameWireFormat() {
    assertEquals(
      List.of(
        "codespace:String",
        "correlationId:String",
        "datasetFileHandle:String",
        "fileCreatedTimestamp:String",
        "importType:String",
        "referential:String",
        "rutebankenFileHandle:String",
        "validationClient:String",
        "validationProfile:String",
        "validationReportId:String",
        "validationStage:String"
      ),
      fieldSignature(ValidationContext.class),
      "ValidationContext is stored inside ValidationState, so changing it also needs a new cache name"
    );
  }

  /**
   * The shape the expectations above describe actually survives the codec that is configured for it.
   * Guards against a field type Kryo cannot handle, which would otherwise surface only in production.
   */
  @Test
  void theCurrentShapeRoundTripsThroughTheConfiguredCodec() throws Exception {
    ValidationContext context = ValidationContext
      .builder()
      .referential("rb_flb")
      .codespace("flb")
      .validationReportId("rb_flb_20260811103000000000")
      .correlationId("correlation-1")
      .build();
    ValidationState state = new ValidationState(
      context,
      Instant.parse("2026-08-11T10:30:00Z")
    );
    state.setHasErrorInCommonFile(true);

    Kryo5Codec codec = new Kryo5Codec();
    ValidationState decoded = (ValidationState) codec
      .getValueDecoder()
      .decode(codec.getValueEncoder().encode(state), new State());

    assertTrue(decoded.hasErrorInCommonFile());
    assertEquals(
      Instant.parse("2026-08-11T10:30:00Z"),
      decoded.getLastProgressAt()
    );
    assertEquals(
      "rb_flb_20260811103000000000",
      decoded.getContext().validationReportId()
    );
    assertEquals("flb", decoded.getContext().codespace());
  }
}
