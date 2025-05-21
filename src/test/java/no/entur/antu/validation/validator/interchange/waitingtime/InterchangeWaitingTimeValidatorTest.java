package no.entur.antu.validation.validator.interchange.waitingtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.entur.antu.common.repository.TestNetexDataRepository;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.validation.validator.SimpleValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

class InterchangeWaitingTimeValidatorTest {

  private TestNetexDataRepository netexDataRepository;
  private static final String CODESPACE = "codespace";

  // Test case identifiers
  private static final String NO_SHARED_ACTIVE_DATE_WITH_UNSATISIFIED_WAITING_TIME =
    "idNoSharedActiveDateWithUnsatisifiedWaitingTime";
  private static final String NO_SHARED_ACTIVE_DATE_WITH_SATISFIED_WAITING_TIME =
    "idNoSharedActiveDateWithSatisfiedWaitingTime";
  private static final String DEPARTURE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME =
    "idDepartureDayOffsetWithSatisifiedWaitingTime";
  private static final String DEPARTURE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME =
    "idDepartureDayOffsetWithUnsatisifiedWaitingTime";
  private static final String ARRIVAL_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME =
    "idArrivalDayOffsetWithSatisifiedWaitingTime";
  private static final String ARRIVAL_DAY_OFF_SET_WITH_UNSATISFIED_WAITING_TIME =
    "idArrivalDayOffsetWithUnsatisifiedWaitingTime";
  private static final String ACTUAL_WAITING_TIME_EXCEEDING_ERROR_TRESHOLD =
    "idActualWaitingTimeExceedingErrorThreshold";
  private static final String ACTUAL_WAITING_TIME_EXCEEDING_WARNING_TRESHOLD =
    "idActualWaitingTimeExceedingWarningThreshold";
  private static final String NO_INTERCHANGE_POSSIBLE =
    "idNoInterchangePossible";
  private static final String NO_GUARANTEED_WARNING =
      "idNoGuaranteedWarning";

  private static String serviceJourneyInterchangeId =
    "ServiceJourneyInterchange:1";

  private static String fromJourneyId = "Test:ServiceJourney:1";
  private static String toJourneyId = "Test:ServiceJourney:2";

  private static int fromStopPoint = 1;
  private static int toStopPoint = 2;

  @BeforeEach
  void setUp() {
    this.netexDataRepository = new TestNetexDataRepository();
  }

  private ScheduledStopPointRefStructure createStopPointRef(int stopPointId) {
    return new ScheduledStopPointRefStructure()
      .withRef(
        NetexEntitiesTestFactory
          .createScheduledStopPointRef(stopPointId)
          .getRef()
      );
  }

  private ServiceJourneyStop createArrivalStop(
    int stopPointId,
    int hour,
    int minute,
    int second,
    Optional<Integer> dayOffset
  ) {
    ScheduledStopPointId stopId = ScheduledStopPointId.of(
      NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId)
    );

    TimetabledPassingTime passingTime = new TimetabledPassingTime()
      .withArrivalTime(LocalTime.of(hour, minute, second));

    if (dayOffset.isPresent()) {
      passingTime =
        passingTime.withArrivalDayOffset(BigInteger.valueOf(dayOffset.get()));
    }
    return ServiceJourneyStop.of(stopId, passingTime);
  }

  private ServiceJourneyStop createDepartureStop(
    int stopPointId,
    int hour,
    int minute,
    int second,
    Optional<Integer> dayOffset
  ) {
    ScheduledStopPointId stopId = ScheduledStopPointId.of(
      NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId)
    );

    TimetabledPassingTime passingTime = new TimetabledPassingTime()
      .withDepartureTime(LocalTime.of(hour, minute, second));

    if (dayOffset.isPresent()) {
      passingTime =
        passingTime.withDepartureDayOffset(BigInteger.valueOf(dayOffset.get()));
    }

    return ServiceJourneyStop.of(stopId, passingTime);
  }

  private ServiceJourneyInterchange createInterchangeWithMaximumWaitTime(
    Duration maximumWaitTime
  ) {
    return new ServiceJourneyInterchange()
      .withId(serviceJourneyInterchangeId)
      .withMaximumWaitTime(maximumWaitTime)
      .withFromJourneyRef(
        new VehicleJourneyRefStructure()
          .withRef(ServiceJourneyId.ofValidId(fromJourneyId).id())
      )
      .withToJourneyRef(
        new VehicleJourneyRefStructure()
          .withRef(ServiceJourneyId.ofValidId(toJourneyId).id())
      )
      .withFromPointRef(createStopPointRef(fromStopPoint))
      .withToPointRef(createStopPointRef(toStopPoint))
      .withGuaranteed(true);
  }

  private void setupTestData(
    String testCaseId,
    ServiceJourneyInterchange interchange,
    Map<String, List<LocalDateTime>> activeDates,
    Map<String, ServiceJourneyStop> stops
  ) {
    // Add interchange info
    netexDataRepository.addServiceJourneyInterchangeInfo(
      testCaseId,
      ServiceJourneyInterchangeInfo.of("", interchange)
    );

    // Convert and add active dates
    Map<ServiceJourneyId, List<LocalDateTime>> journeyActiveDates = activeDates
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(
          entry -> ServiceJourneyId.ofValidId(entry.getKey()),
          Map.Entry::getValue
        )
      );
    netexDataRepository.putServiceJourneyIdToActiveDates(
      testCaseId,
      journeyActiveDates
    );

    // Convert and add journey stops
    Map<ServiceJourneyId, List<ServiceJourneyStop>> journeyStops = stops
      .entrySet()
      .stream()
      .collect(
        Collectors.toMap(
          entry -> ServiceJourneyId.ofValidId(entry.getKey()),
          entry -> List.of(entry.getValue())
        )
      );
    netexDataRepository.putServiceJourneyStop(testCaseId, journeyStops);
  }

  private void setupTestCaseWithNoSharedDateUnsatisfiedWaitingTime() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ZERO);

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 14, 0, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 14, 0, 0, Optional.empty())
    );

    setupTestData(
      NO_SHARED_ACTIVE_DATE_WITH_UNSATISIFIED_WAITING_TIME,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithNoSharedDateSatisfiedWaitingTime() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofHours(1));

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 23, 45, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 0, 15, 0, Optional.empty())
    );

    setupTestData(
      NO_SHARED_ACTIVE_DATE_WITH_SATISFIED_WAITING_TIME,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithDepartureDayOffsetSatisfiedWaitingTime() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofHours(1));

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 23, 45, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 0, 15, 0, Optional.of(1))
    );

    setupTestData(
      DEPARTURE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithDepartureDayOffsetUnsatisfiedWaitingTime() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofHours(1));

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 23, 45, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 0, 46, 0, Optional.of(1))
    );

    setupTestData(
      DEPARTURE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithArrivalDayOffsetSatisfiedWaitingTime() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofHours(1));

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 3, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 23, 45, 0, Optional.of(1)),
      toJourneyId,
      createDepartureStop(toStopPoint, 0, 15, 0, Optional.empty())
    );

    setupTestData(
      ARRIVAL_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithArrivalDayOffsetUnsatisfiedWaitingTime() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofMinutes(29));

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 3, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 23, 45, 0, Optional.of(1)),
      toJourneyId,
      createDepartureStop(toStopPoint, 0, 15, 0, Optional.empty())
    );

    setupTestData(
      ARRIVAL_DAY_OFF_SET_WITH_UNSATISFIED_WAITING_TIME,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithActualWaitingTimeExceedingErrorTreshold() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofHours(1));
    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 14, 0, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 17, 0, 0, Optional.empty())
    );
    setupTestData(
      ACTUAL_WAITING_TIME_EXCEEDING_ERROR_TRESHOLD,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithActualWaitingTimeExceedingWarningTreshold() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofHours(1));
    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 14, 0, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 16, 59, 0, Optional.empty())
    );
    setupTestData(
      ACTUAL_WAITING_TIME_EXCEEDING_WARNING_TRESHOLD,
      interchange,
      activeDates,
      stops
    );
  }

  private void setupTestCaseWithNoInterchangePossible() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime(Duration.ofHours(1));
    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 14, 0, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 16, 59, 0, Optional.empty())
    );
    setupTestData(NO_INTERCHANGE_POSSIBLE, interchange, activeDates, stops);
  }

  @Test
  void testNoSharedActiveDateWithUnsatisfiedWaitingTimeGivesValidationError() {
    setupTestCaseWithNoSharedDateUnsatisfiedWaitingTime();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      NO_SHARED_ACTIVE_DATE_WITH_UNSATISIFIED_WAITING_TIME
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
      .getValidationReportEntries()
      .stream()
      .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
      InterchangeWaitingTimeValidator.RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_ERROR.name(),
      validationReportEntries.get(0).getName()
    );
  }

  @Test
  void testNoSharedActiveDateWithSatisfiedWaitingTimeGivesNoValidationError() {
    setupTestCaseWithNoSharedDateSatisfiedWaitingTime();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      NO_SHARED_ACTIVE_DATE_WITH_SATISFIED_WAITING_TIME
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    assertTrue(resultingReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testDepartureDayOffsetWithSatisfiedWaitingTimeGivesNoValidationError() {
    setupTestCaseWithDepartureDayOffsetSatisfiedWaitingTime();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      DEPARTURE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    assertTrue(resultingReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testDepartureDayOffsetWithUnsatisfiedWaitingTimeButNotExceedingErrorTresholdGivesValidationWarning() {
    setupTestCaseWithDepartureDayOffsetUnsatisfiedWaitingTime();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      DEPARTURE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
      .getValidationReportEntries()
      .stream()
      .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
      InterchangeWaitingTimeValidator.RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING.name(),
      validationReportEntries.get(0).getName()
    );
  }

  @Test
  void testArrivalDayOffsetWithSatisfiedWaitingTimeGivesNoValidationError() {
    setupTestCaseWithArrivalDayOffsetSatisfiedWaitingTime();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      ARRIVAL_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    assertTrue(resultingReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testArrivalDayOffsetWithUnsatisfiedWaitingTimeButNotExceedingErrorTresholdGivesValidationWarning() {
    setupTestCaseWithArrivalDayOffsetUnsatisfiedWaitingTime();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      ARRIVAL_DAY_OFF_SET_WITH_UNSATISFIED_WAITING_TIME
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
      .getValidationReportEntries()
      .stream()
      .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
      InterchangeWaitingTimeValidator.RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING.name(),
      validationReportEntries.get(0).getName()
    );
  }

  @Test
  void testActualWaitingTimeExceedingErrorTresholdGivesValidationError() {
    setupTestCaseWithActualWaitingTimeExceedingErrorTreshold();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      ACTUAL_WAITING_TIME_EXCEEDING_ERROR_TRESHOLD
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
      .getValidationReportEntries()
      .stream()
      .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
      InterchangeWaitingTimeValidator.RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_ERROR.name(),
      validationReportEntries.get(0).getName()
    );
  }

  @Test
  void testActualWaitingTimeExceedingWarningTresholdGivesValidationWarning() {
    setupTestCaseWithActualWaitingTimeExceedingWarningTreshold();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      ACTUAL_WAITING_TIME_EXCEEDING_WARNING_TRESHOLD
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
      .getValidationReportEntries()
      .stream()
      .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
      InterchangeWaitingTimeValidator.RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING.name(),
      validationReportEntries.get(0).getName()
    );
  }

  @Test
  void testNoInterchangePossible() {
    setupTestCaseWithNoInterchangePossible();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      NO_INTERCHANGE_POSSIBLE
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
      .getValidationReportEntries()
      .stream()
      .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
      InterchangeWaitingTimeValidator.RULE_NO_INTERCHANGE_POSSIBLE.name(),
      validationReportEntries.get(0).getName()
    );
  }

  @Test
  void testMinimumWaitTime() {
    Duration minimum = new InterchangeWaitingTimeValidator(
      new SimpleValidationEntryFactory(),
      netexDataRepository
    )
      .getShortestActualWaitingTimeForInterchange(
        List
          .of(
            LocalDateTime.of(2025, 1, 5, 12, 0, 0),
            LocalDateTime.of(2025, 1, 6, 12, 0, 0),
            LocalDateTime.of(2025, 1, 7, 12, 0, 0),
            LocalDateTime.of(2025, 1, 8, 12, 0, 0)
          )
          .stream()
          .sorted()
          .collect(Collectors.toUnmodifiableList()),
        List
          .of(
            LocalDateTime.of(2025, 1, 1, 11, 5, 0),
            LocalDateTime.of(2025, 1, 2, 11, 15, 0),
            LocalDateTime.of(2025, 1, 3, 11, 20, 0),
            LocalDateTime.of(2025, 1, 4, 11, 25, 0)
          )
          .stream()
          .sorted()
          .collect(Collectors.toUnmodifiableList())
      );
    assertEquals(minimum, null);
  }

  @Test
  void testNotGuaranteedGivesWarning() {
    ServiceJourneyInterchange interchange = new ServiceJourneyInterchange().withGuaranteed(false);
    setupTestData(NO_GUARANTEED_WARNING, interchange, Map.of(), Map.of());

    InterchangeWaitingTimeValidator validator =
        new InterchangeWaitingTimeValidator(
            new SimpleValidationEntryFactory(),
            netexDataRepository
        );
    ValidationReport validationReport = new ValidationReport(
        CODESPACE,
        NO_GUARANTEED_WARNING
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
        .getValidationReportEntries()
        .stream()
        .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
        InterchangeWaitingTimeValidator.RULE_INTERCHANGE_NOT_GUARANTEED.name(),
        validationReportEntries.get(0).getName()
    );
  }


  @Test
  void testGuaranteedUnsetGivesWarning() {
    ServiceJourneyInterchange interchange = new ServiceJourneyInterchange();
    setupTestData(NO_GUARANTEED_WARNING, interchange, Map.of(), Map.of());

    InterchangeWaitingTimeValidator validator =
        new InterchangeWaitingTimeValidator(
            new SimpleValidationEntryFactory(),
            netexDataRepository
        );
    ValidationReport validationReport = new ValidationReport(
        CODESPACE,
        NO_GUARANTEED_WARNING
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    List<ValidationReportEntry> validationReportEntries = resultingReport
        .getValidationReportEntries()
        .stream()
        .toList();
    assertEquals(1, validationReportEntries.size());
    assertEquals(
        InterchangeWaitingTimeValidator.RULE_INTERCHANGE_NOT_GUARANTEED.name(),
        validationReportEntries.get(0).getName()
    );
  }
}
