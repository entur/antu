package no.entur.antu.pubsub;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import no.entur.antu.Constants;
import no.entur.antu.job.JobType;
import no.entur.antu.job.ValidationStatus;
import org.junit.jupiter.api.Test;

/**
 * Pins every string antu is matched on from outside this repository.
 *
 * <p>marduk and kakka read the PubSub attributes and the status body; marduk's subscription filters on
 * {@code EnturValidationClient}; the version of antu already deployed reads the job attributes off the
 * shared queue while a new one rolls out; and the published report is fetched by path. Changing anything
 * here is a coordinated release, not a refactor.
 *
 * <p>The expected values are written out as literals rather than read through the constants, which is the
 * whole point of this class. The other tests take the key from the same constant they mean to pin
 * ({@code attributes.get(NETEX_FILE_NAME)}), so both sides move together and the assertion holds whatever
 * the constant says. Renaming the value of {@code NETEX_FILE_NAME}, or a {@code JobType} wire value, or
 * the {@code timeout} status, used to leave the entire suite green.
 */
class WireContractTest {

  /**
   * Attribute names on {@code AntuNetexValidationQueue}, {@code AntuJobQueue} and
   * {@code AntuNetexValidationStatusQueue}.
   */
  @Test
  void pubSubAttributeNames() {
    assertAll(
      () -> assertEquals("JOB_TYPE", Constants.JOB_TYPE),
      () -> assertEquals("EnturNetexFileName", Constants.NETEX_FILE_NAME),
      () ->
        assertEquals("EnturDatasetReferential", Constants.DATASET_REFERENTIAL),
      () -> assertEquals("EnturDatasetCodespace", Constants.DATASET_CODESPACE),
      () ->
        assertEquals(
          "EnturDatasetNbNetexFiles",
          Constants.DATASET_NB_NETEX_FILES
        ),
      () ->
        assertEquals(
          "EnturDatasetNbCommonFiles",
          Constants.DATASET_NB_COMMON_FILES
        ),
      () ->
        assertEquals(
          "EnturValidationReportId",
          Constants.VALIDATION_REPORT_ID_HEADER
        ),
      () ->
        assertEquals(
          "EnturValidationCorrelationId",
          Constants.VALIDATION_CORRELATION_ID_HEADER
        ),
      () ->
        assertEquals(
          "EnturValidationProfile",
          Constants.VALIDATION_PROFILE_HEADER
        ),
      () ->
        assertEquals(
          "EnturValidationClient",
          Constants.VALIDATION_CLIENT_HEADER
        ),
      () ->
        assertEquals("EnturValidationStage", Constants.VALIDATION_STAGE_HEADER),
      () ->
        assertEquals(
          "EnturValidationImportType",
          Constants.VALIDATION_IMPORT_TYPE
        ),
      () ->
        assertEquals(
          "EnturValidationDatasetFileHandle",
          Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER
        ),
      () ->
        assertEquals(
          "FileCreatedTimestamp",
          Constants.FILE_CREATED_TIMESTAMP_HEADER
        ),
      () ->
        assertEquals(
          "RutebankenFileHandle",
          Constants.RUTEBANKEN_FILE_HANDLE_HEADER
        )
    );
  }

  /**
   * marduk's subscription on the status queue filters on
   * {@code attributes.EnturValidationClient = "Marduk"}, so a change here means marduk stops receiving
   * anything at all, with no error on either side.
   */
  @Test
  void validationClientNames() {
    assertAll(
      () -> assertEquals("Marduk", Constants.VALIDATION_CLIENT_MARDUK),
      () -> assertEquals("Kakka", Constants.VALIDATION_CLIENT_KAKKA)
    );
  }

  /**
   * The {@code JOB_TYPE} attribute values. Frozen independently of the enum constant names, so that
   * renaming a constant is not a wire change: {@code CREATE_LINE_FILE_JOBS} still says
   * {@code AGGREGATE_COMMON_FILES} from when it did aggregate common files.
   */
  @Test
  void jobTypeWireValues() {
    Map<JobType, String> expected = new LinkedHashMap<>();
    expected.put(JobType.SPLIT, "SPLIT");
    expected.put(JobType.VALIDATE, "VALIDATE");
    expected.put(JobType.VALIDATE_DATASET, "VALIDATE_DATASET");
    expected.put(JobType.COMPLETE_VALIDATION, "COMPLETE_VALIDATION");
    expected.put(JobType.AGGREGATE_REPORTS, "AGGREGATE_REPORTS");
    expected.put(JobType.CREATE_LINE_FILE_JOBS, "AGGREGATE_COMMON_FILES");
    expected.put(JobType.REFRESH_STOP_PLACE_CACHE, "REFRESH_STOP_PLACE_CACHE");
    expected.put(
      JobType.REFRESH_ORGANISATION_ALIAS_CACHE,
      "REFRESH_ORGANISATION_ALIAS_CACHE"
    );

    assertEquals(
      EnumSet.allOf(JobType.class),
      EnumSet.copyOf(expected.keySet()),
      "a new job type has to be pinned here before it can go on the wire"
    );
    assertAll(
      expected
        .entrySet()
        .stream()
        .map(entry ->
          () -> assertEquals(entry.getValue(), entry.getKey().wireValue())
        )
    );
  }

  /**
   * The message body on {@code AntuNetexValidationStatusQueue}. marduk matches these literally and logs
   * and discards anything else, so an unrecognised value leaves the import with no terminal state.
   */
  @Test
  void validationStatusWireValues() {
    Map<ValidationStatus, String> expected = new LinkedHashMap<>();
    expected.put(ValidationStatus.STARTED, "started");
    expected.put(ValidationStatus.OK, "ok");
    expected.put(ValidationStatus.FAILED, "failed");
    expected.put(ValidationStatus.TIMEOUT, "timeout");

    assertEquals(
      EnumSet.allOf(ValidationStatus.class),
      EnumSet.copyOf(expected.keySet()),
      "a new status has to be pinned here, and understood by marduk, before it can be sent"
    );
    assertAll(
      expected
        .entrySet()
        .stream()
        .map(entry ->
          () -> assertEquals(entry.getValue(), entry.getKey().wireValue())
        )
    );
  }

  /**
   * Separates the NeTEx file names in a job message body. Both versions of antu have to agree on it
   * across a rollout, and a NeTEx file name may not contain it.
   */
  @Test
  void fileNameDelimiter() {
    assertEquals("§", Constants.FILENAME_DELIMITER);
  }

  @Test
  void queueNames() {
    assertAll(
      () ->
        assertEquals(
          "AntuNetexValidationQueue",
          AntuQueues.NETEX_VALIDATION_QUEUE
        ),
      () ->
        assertEquals(
          "AntuNetexValidationStatusQueue",
          AntuQueues.NETEX_VALIDATION_STATUS_QUEUE
        ),
      () -> assertEquals("AntuJobQueue", AntuQueues.JOB_QUEUE)
    );
  }

  /**
   * The published report is read out of the bucket by path by the REST endpoint and by the validation
   * clients, so the layout is as much a contract as the messages are. Note that the path component is the
   * referential ({@code rb_flb}), not the codespace.
   */
  @Test
  void publishedReportLayout() {
    assertAll(
      () -> assertEquals("reports/", Constants.BLOBSTORE_PATH_ANTU_REPORTS),
      () ->
        assertEquals("/validation-report-", Constants.VALIDATION_REPORT_PREFIX),
      () -> assertEquals(".json", Constants.VALIDATION_REPORT_SUFFIX),
      () -> assertEquals(".status", Constants.VALIDATION_REPORT_STATUS_SUFFIX),
      () ->
        assertEquals(
          "reports/rb_flb/validation-report-rb_flb_20260811103000000000.json",
          Constants.BLOBSTORE_PATH_ANTU_REPORTS +
          "rb_flb" +
          Constants.VALIDATION_REPORT_PREFIX +
          "rb_flb_20260811103000000000" +
          Constants.VALIDATION_REPORT_SUFFIX
        )
    );
  }
}
