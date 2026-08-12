package no.entur.antu.job;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The kind of work a message on the job queue asks for.
 *
 * <p>The string each constant carries is written to the {@code JOB_TYPE} PubSub attribute, and is frozen:
 * messages already on the queue have to stay readable across a deploy. It is deliberately spelled out
 * rather than derived from {@link #name()}, which would make renaming a constant a wire change.
 */
public enum JobType {
  SPLIT("SPLIT"),
  VALIDATE("VALIDATE"),
  VALIDATE_DATASET("VALIDATE_DATASET"),
  COMPLETE_VALIDATION("COMPLETE_VALIDATION"),
  AGGREGATE_REPORTS("AGGREGATE_REPORTS"),
  /**
   * Nothing aggregates common files any more; the name on the wire is kept from when it did.
   */
  CREATE_LINE_FILE_JOBS("AGGREGATE_COMMON_FILES"),
  REFRESH_STOP_PLACE_CACHE("REFRESH_STOP_PLACE_CACHE"),
  REFRESH_ORGANISATION_ALIAS_CACHE("REFRESH_ORGANISATION_ALIAS_CACHE");

  private static final Map<String, JobType> BY_WIRE_VALUE = Stream
    .of(values())
    .collect(Collectors.toMap(JobType::wireValue, Function.identity()));

  private final String wireValue;

  JobType(String wireValue) {
    this.wireValue = wireValue;
  }

  public String wireValue() {
    return wireValue;
  }

  public static Optional<JobType> fromWireValue(String wireValue) {
    return Optional.ofNullable(BY_WIRE_VALUE.get(wireValue));
  }
}
