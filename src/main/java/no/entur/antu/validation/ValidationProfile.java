package no.entur.antu.validation;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * A validation profile is a collection of validation rules customized for a specific validation
 * use case.
 */
public enum ValidationProfile {
  /**
   * Default profile for timetable data.
   */
  TIMETABLE("Timetable"),

  /**
   * Profile for flexible lines created in NPlan.
   */
  TIMETABLE_FLEXIBLE_TRANSPORT("TimetableFlexibleTransport"),

  /**
   * Profile for flexible lines imported through the operator portal.
   */
  IMPORT_TIMETABLE_FLEXIBLE_TRANSPORT("ImportTimetableFlexibleTransport"),

  /**
   * Profile for validating a dataset that contains both static data and flex data.
   */
  TIMETABLE_FLEXIBLE_TRANSPORT_MERGING("TimetableFlexibleTransportMerging"),

  /**
   * Profile for validating swedish timetable data.
   */
  TIMETABLE_SWEDEN("TimetableSweden"),

  /**
   * Profile for stop dataset.
   */
  STOP("Stop");

  private final String id;

  ValidationProfile(String id) {
    this.id = Objects.requireNonNull(id);
  }

  /**
   * Return a ValidationProfile for a given name if it exists.
   */
  public static Optional<ValidationProfile> findById(String validationProfile) {
    return EnumSet
      .allOf(ValidationProfile.class)
      .stream()
      .filter(element -> element.id().equalsIgnoreCase(validationProfile))
      .findFirst();
  }

  public String id() {
    return id;
  }
}
