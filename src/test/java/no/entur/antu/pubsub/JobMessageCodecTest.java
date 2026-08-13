package no.entur.antu.pubsub;

import static no.entur.antu.Constants.DATASET_NB_COMMON_FILES;
import static no.entur.antu.Constants.DATASET_NB_NETEX_FILES;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.JOB_TYPE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_CORRELATION_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import no.entur.antu.exception.AntuException;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.ValidationContext;
import org.junit.jupiter.api.Test;

class JobMessageCodecTest {

  private static final ValidationContext CONTEXT = ValidationContext
    .builder()
    .referential("rb_flb")
    .codespace("flb")
    .validationReportId("rb_flb_20260811103000000000")
    .correlationId("correlation-1")
    .validationProfile("timetable")
    .validationClient("Marduk")
    .validationStage("EnturValidationStagePreValidation")
    .importType("netex-flex")
    .datasetFileHandle("inbound/received/flb/dataset.zip")
    .fileCreatedTimestamp("2026-08-11T10:30:00Z")
    .rutebankenFileHandle("rutebanken/dataset.zip")
    .build();

  @Test
  void splitDatasetRoundTrips() {
    assertRoundTrips(new AntuJob.SplitDataset(CONTEXT));
  }

  @Test
  void validateFileRoundTrips() {
    assertRoundTrips(
      new AntuJob.ValidateFile(
        CONTEXT,
        "_common.xml",
        12,
        2,
        List.of("_common.xml", "line.xml")
      )
    );
  }

  @Test
  void createLineFileJobsRoundTrips() {
    assertRoundTrips(
      new AntuJob.CreateLineFileJobs(
        CONTEXT,
        List.of("_common.xml", "a.xml", "b.xml")
      )
    );
  }

  @Test
  void aggregateReportsRoundTrips() {
    assertRoundTrips(
      new AntuJob.AggregateReports(CONTEXT, List.of("a.xml", "b.xml"))
    );
  }

  @Test
  void validateDatasetRoundTrips() {
    assertRoundTrips(new AntuJob.ValidateDataset(CONTEXT));
  }

  @Test
  void completeValidationRoundTrips() {
    assertRoundTrips(new AntuJob.CompleteValidation(CONTEXT));
  }

  @Test
  void cacheRefreshJobsRoundTrip() {
    assertRoundTrips(new AntuJob.RefreshStopPlaceCache());
    assertRoundTrips(new AntuJob.RefreshOrganisationAliasCache());
  }

  /**
   * These attribute names are the contract with marduk and kakka, and with the antu version already
   * deployed while a new one rolls out. Renaming one silently drops messages.
   */
  @Test
  void attributeNamesAreTheOnesOnTheWire() {
    Map<String, String> attributes = JobMessageCodec
      .encode(new AntuJob.ValidateFile(CONTEXT, "line.xml", 12, 2, List.of()))
      .attributes();

    assertEquals("VALIDATE", attributes.get(JOB_TYPE));
    assertEquals("rb_flb", attributes.get(DATASET_REFERENTIAL));
    assertEquals(
      "rb_flb_20260811103000000000",
      attributes.get(VALIDATION_REPORT_ID_HEADER)
    );
    assertEquals(
      "correlation-1",
      attributes.get(VALIDATION_CORRELATION_ID_HEADER)
    );
    assertEquals(
      "inbound/received/flb/dataset.zip",
      attributes.get(VALIDATION_DATASET_FILE_HANDLE_HEADER)
    );
    assertEquals("line.xml", attributes.get(NETEX_FILE_NAME));
    assertEquals("12", attributes.get(DATASET_NB_NETEX_FILES));
    assertEquals("2", attributes.get(DATASET_NB_COMMON_FILES));
  }

  /**
   * PubSub caps an attribute value at 1024 bytes, which a real dataset's file list blows past, so the
   * list has to be in the body.
   */
  @Test
  void theFileNameListTravelsInTheBodyNotInAnAttribute() {
    List<String> fileNames = IntStream
      .range(0, 200)
      .mapToObj(i -> "AVI_AVI-Line-" + i + "_a_long_enough_file_name.xml")
      .toList();

    JobMessageCodec.JobMessage message = JobMessageCodec.encode(
      new AntuJob.AggregateReports(CONTEXT, fileNames)
    );

    assertTrue(message.body().length() > 1024);
    assertTrue(
      message
        .attributes()
        .values()
        .stream()
        .allMatch(value -> value.getBytes().length <= 1024)
    );
    assertEquals(
      fileNames,
      (
        (AntuJob.AggregateReports) JobMessageCodec.decode(
          message.attributes(),
          message.body()
        )
      ).netexFileNames()
    );
  }

  @Test
  void jobsWithoutAFileListHaveAnEmptyBody() {
    assertTrue(
      JobMessageCodec.encode(new AntuJob.SplitDataset(CONTEXT)).body().isEmpty()
    );
  }

  @Test
  void anEmptyBodyDecodesToAnEmptyList() {
    assertEquals(List.of(), JobMessageCodec.splitFileNames(""));
    assertEquals(List.of(), JobMessageCodec.splitFileNames(null));
  }

  @Test
  void anUnknownJobTypeIsRejected() {
    Map<String, String> unknownJobType = Map.of(JOB_TYPE, "NO_SUCH_JOB");
    Map<String, String> noJobType = Map.of();

    assertThrows(
      AntuException.class,
      () -> JobMessageCodec.decode(unknownJobType, "")
    );
    assertThrows(
      AntuException.class,
      () -> JobMessageCodec.decode(noJobType, "")
    );
  }

  /**
   * A request from a validation client carries no report id: antu derives one.
   */
  @Test
  void aValidationRequestNeedsNoReportId() {
    ValidationContext context = JobMessageCodec.readContext(
      Map.of(
        DATASET_REFERENTIAL,
        "rb_flb",
        VALIDATION_DATASET_FILE_HANDLE_HEADER,
        "inbound/received/flb/dataset.zip"
      )
    );

    assertEquals("rb_flb", context.referential());
    assertEquals(
      "inbound/received/flb/dataset.zip",
      context.datasetFileHandle()
    );
    assertEquals(null, context.validationReportId());
  }

  @Test
  void emptyContextFieldsAreLeftOutOfTheAttributes() {
    Map<String, String> attributes = JobMessageCodec.statusAttributes(
      ValidationContext.builder().referential("rb_flb").build()
    );

    assertEquals(Map.of(DATASET_REFERENTIAL, "rb_flb"), attributes);
    assertFalse(attributes.containsKey(VALIDATION_REPORT_ID_HEADER));
  }

  private static void assertRoundTrips(AntuJob job) {
    JobMessageCodec.JobMessage message = JobMessageCodec.encode(job);
    assertEquals(
      job,
      JobMessageCodec.decode(message.attributes(), message.body())
    );
  }
}
