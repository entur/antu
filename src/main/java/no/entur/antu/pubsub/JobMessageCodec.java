package no.entur.antu.pubsub;

import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.DATASET_NB_COMMON_FILES;
import static no.entur.antu.Constants.DATASET_NB_NETEX_FILES;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.FILENAME_DELIMITER;
import static no.entur.antu.Constants.FILE_CREATED_TIMESTAMP_HEADER;
import static no.entur.antu.Constants.JOB_TYPE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.RUTEBANKEN_FILE_HANDLE_HEADER;
import static no.entur.antu.Constants.VALIDATION_CLIENT_HEADER;
import static no.entur.antu.Constants.VALIDATION_CORRELATION_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER;
import static no.entur.antu.Constants.VALIDATION_IMPORT_TYPE;
import static no.entur.antu.Constants.VALIDATION_PROFILE_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_STAGE_HEADER;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import no.entur.antu.exception.AntuException;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobType;
import no.entur.antu.job.ValidationContext;

/**
 * Translates jobs to and from the PubSub representation: scalar fields go into message attributes,
 * the NeTEx file name list goes into the message body.
 *
 * <p>A NeTEx file name list outgrows the 1024 byte attribute value limit on any real dataset, which
 * is why it travels in the body. The attribute names are the ones marduk and kakka already use and
 * are also what previously deployed antu versions wrote, so messages stay readable across a deploy.
 */
public final class JobMessageCodec {

  private JobMessageCodec() {}

  /**
   * A PubSub message as antu produces and consumes it.
   */
  public record JobMessage(Map<String, String> attributes, String body) {
    public JobMessage {
      attributes = Map.copyOf(attributes);
    }
  }

  /**
   * The bodyless arms bind a name they never use (java:S1481). An unnamed pattern would say it better, but
   * prettier-java cannot parse one, and a {@code default} arm would give up the exhaustiveness check that
   * makes adding a job type a compile error.
   */
  @SuppressWarnings("java:S1481")
  public static JobMessage encode(AntuJob job) {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put(JOB_TYPE, job.type().wireValue());

    if (job instanceof AntuJob.ValidationJob validationJob) {
      putContext(attributes, validationJob.context());
    }
    if (job instanceof AntuJob.ValidateFile validateFile) {
      put(attributes, NETEX_FILE_NAME, validateFile.netexFileName());
      put(
        attributes,
        DATASET_NB_NETEX_FILES,
        String.valueOf(validateFile.nbNetexFiles())
      );
      put(
        attributes,
        DATASET_NB_COMMON_FILES,
        String.valueOf(validateFile.nbCommonFiles())
      );
    }

    // Only the file name lists need the body; an attribute value is capped at 1024 bytes. Every other job
    // travels entirely in its attributes. Listed one by one rather than defaulted, so adding a job type
    // does not silently inherit an empty body.
    String body =
      switch (job) {
        case AntuJob.ValidateFile validateFile -> joinFileNames(
          validateFile.allNetexFileNames()
        );
        case AntuJob.CreateLineFileJobs createLineFileJobs -> joinFileNames(
          createLineFileJobs.allNetexFileNames()
        );
        case AntuJob.AggregateReports aggregateReports -> joinFileNames(
          aggregateReports.netexFileNames()
        );
        case AntuJob.SplitDataset ignored -> "";
        case AntuJob.ValidateDataset ignored -> "";
        case AntuJob.CompleteValidation ignored -> "";
        case AntuJob.RefreshStopPlaceCache ignored -> "";
        case AntuJob.RefreshOrganisationAliasCache ignored -> "";
        case AntuJob.RefreshVehicleReferenceCache ignored -> "";
      };
    return new JobMessage(attributes, body);
  }

  public static AntuJob decode(Map<String, String> attributes, String body) {
    String rawJobType = attributes.get(JOB_TYPE);
    if (rawJobType == null) {
      throw new AntuException("Missing " + JOB_TYPE + " attribute");
    }
    JobType jobType = JobType
      .fromWireValue(rawJobType)
      .orElseThrow(() -> new AntuException("Unknown job type " + rawJobType));

    return switch (jobType) {
      case SPLIT -> new AntuJob.SplitDataset(readContext(attributes));
      case VALIDATE -> new AntuJob.ValidateFile(
        readContext(attributes),
        attributes.get(NETEX_FILE_NAME),
        readInt(attributes, DATASET_NB_NETEX_FILES),
        readInt(attributes, DATASET_NB_COMMON_FILES),
        splitFileNames(body)
      );
      case CREATE_LINE_FILE_JOBS -> new AntuJob.CreateLineFileJobs(
        readContext(attributes),
        splitFileNames(body)
      );
      case AGGREGATE_REPORTS -> new AntuJob.AggregateReports(
        readContext(attributes),
        splitFileNames(body)
      );
      case VALIDATE_DATASET -> new AntuJob.ValidateDataset(
        readContext(attributes)
      );
      case COMPLETE_VALIDATION -> new AntuJob.CompleteValidation(
        readContext(attributes)
      );
      case REFRESH_STOP_PLACE_CACHE -> new AntuJob.RefreshStopPlaceCache();
      case REFRESH_ORGANISATION_ALIAS_CACHE -> new AntuJob.RefreshOrganisationAliasCache();
      case REFRESH_VEHICLE_REFERENCE_CACHE -> new AntuJob.RefreshVehicleReferenceCache();
    };
  }

  /**
   * Read the validation context out of an inbound validation request. Unlike a job message, a
   * request carries no report id or codespace: antu derives those when it starts the validation.
   */
  public static ValidationContext readContext(Map<String, String> attributes) {
    return ValidationContext
      .builder()
      .referential(attributes.get(DATASET_REFERENTIAL))
      .codespace(attributes.get(DATASET_CODESPACE))
      .validationReportId(attributes.get(VALIDATION_REPORT_ID_HEADER))
      .correlationId(attributes.get(VALIDATION_CORRELATION_ID_HEADER))
      .validationProfile(attributes.get(VALIDATION_PROFILE_HEADER))
      .validationClient(attributes.get(VALIDATION_CLIENT_HEADER))
      .validationStage(attributes.get(VALIDATION_STAGE_HEADER))
      .importType(attributes.get(VALIDATION_IMPORT_TYPE))
      .datasetFileHandle(attributes.get(VALIDATION_DATASET_FILE_HANDLE_HEADER))
      .fileCreatedTimestamp(attributes.get(FILE_CREATED_TIMESTAMP_HEADER))
      .rutebankenFileHandle(attributes.get(RUTEBANKEN_FILE_HANDLE_HEADER))
      .build();
  }

  /**
   * The attributes echoed back to the validation client along with a status.
   */
  public static Map<String, String> statusAttributes(
    ValidationContext context
  ) {
    Map<String, String> attributes = new LinkedHashMap<>();
    putContext(attributes, context);
    return attributes;
  }

  private static void putContext(
    Map<String, String> attributes,
    ValidationContext context
  ) {
    put(attributes, DATASET_REFERENTIAL, context.referential());
    put(attributes, DATASET_CODESPACE, context.codespace());
    put(attributes, VALIDATION_REPORT_ID_HEADER, context.validationReportId());
    put(attributes, VALIDATION_CORRELATION_ID_HEADER, context.correlationId());
    put(attributes, VALIDATION_PROFILE_HEADER, context.validationProfile());
    put(attributes, VALIDATION_CLIENT_HEADER, context.validationClient());
    put(attributes, VALIDATION_STAGE_HEADER, context.validationStage());
    put(attributes, VALIDATION_IMPORT_TYPE, context.importType());
    put(
      attributes,
      VALIDATION_DATASET_FILE_HANDLE_HEADER,
      context.datasetFileHandle()
    );
    put(
      attributes,
      FILE_CREATED_TIMESTAMP_HEADER,
      context.fileCreatedTimestamp()
    );
    put(
      attributes,
      RUTEBANKEN_FILE_HANDLE_HEADER,
      context.rutebankenFileHandle()
    );
  }

  private static void put(
    Map<String, String> attributes,
    String name,
    String value
  ) {
    if (value != null && !value.isEmpty()) {
      attributes.put(name, value);
    }
  }

  private static int readInt(Map<String, String> attributes, String name) {
    String value = attributes.get(name);
    return value == null || value.isEmpty() ? 0 : Integer.parseInt(value);
  }

  public static String joinFileNames(List<String> fileNames) {
    return String.join(FILENAME_DELIMITER, fileNames);
  }

  public static List<String> splitFileNames(String body) {
    if (body == null || body.isBlank()) {
      return List.of();
    }
    return Arrays
      .stream(body.split(FILENAME_DELIMITER))
      .filter(name -> !name.isBlank())
      .toList();
  }
}
