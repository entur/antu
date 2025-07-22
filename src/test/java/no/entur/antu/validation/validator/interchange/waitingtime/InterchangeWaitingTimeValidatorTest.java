package no.entur.antu.validation.validator.interchange.waitingtime;

import static no.entur.antu.validation.validator.interchange.waitingtime.InterchangeWaitingTimeValidator.sortedLocalDateTimesForServiceJourneyAtStop;
import static org.junit.Assert.*;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.entur.antu.common.repository.TestNetexDataRepository;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.SimpleValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

class InterchangeWaitingTimeValidatorTest {

  private TestNetexDataRepository netexDataRepository;
  private static final String CODESPACE = "codespace";

  // Test case identifiers
  private static final String SATISFIED_WAITING_TIME = "satisfiedWaitingTime";
  private static final String ACTUAL_WAITING_TIME_EXCEEDING_WARNING_TRESHOLD =
    "idActualWaitingTimeExceedingWarningThreshold";
  private static final String NO_INTERCHANGE_POSSIBLE =
    "idNoInterchangePossible";
  private static final String REFERENCE_TO_NON_EXISTENT_FROM_SERVICE_JOURNEY =
    "referenceToNonExistentFromServiceJourney";
  private static final String REFERENCE_TO_NON_EXISTENT_TO_SERVICE_JOURNEY =
    "referenceToNonExistentToServiceJourney";

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
    return ServiceJourneyStop.of(stopId, passingTime, null, null);
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

    return ServiceJourneyStop.of(stopId, passingTime, null, null);
  }

  private ServiceJourneyInterchange createInterchangeWithMaximumWaitTime() {
    return new ServiceJourneyInterchange()
      .withId(serviceJourneyInterchangeId)
      .withFromJourneyRef(
        new VehicleJourneyRefStructure()
          .withRef(ServiceJourneyId.ofValidId(fromJourneyId).id())
      )
      .withToJourneyRef(
        new VehicleJourneyRefStructure()
          .withRef(ServiceJourneyId.ofValidId(toJourneyId).id())
      )
      .withFromPointRef(createStopPointRef(fromStopPoint))
      .withToPointRef(createStopPointRef(toStopPoint));
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

  private void setupTestCaseSatisfiedWaitingTime() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime();

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0)),
      toJourneyId,
      List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 21, 0, 0, Optional.empty()),
      toJourneyId,
      createDepartureStop(toStopPoint, 22, 59, 0, Optional.empty())
    );

    setupTestData(SATISFIED_WAITING_TIME, interchange, activeDates, stops);
  }

  private void setupTestCaseWithActualWaitingTimeExceedingWarningTreshold() {
    ServiceJourneyInterchange interchange =
      createInterchangeWithMaximumWaitTime();
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
      createDepartureStop(toStopPoint, 16, 0, 1, Optional.empty())
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
      createInterchangeWithMaximumWaitTime();
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

  private void setupTestCaseWithReferenceToNonExistentFromServiceJourney() {
    var nonExistentFromJourneyId = "Test:ServiceJourney:999";
    var interchange = new ServiceJourneyInterchange()
      .withId(serviceJourneyInterchangeId)
      .withFromJourneyRef(
        new VehicleJourneyRefStructure()
          .withRef(ServiceJourneyId.ofValidId(nonExistentFromJourneyId).id())
      );
    setupTestData(
      REFERENCE_TO_NON_EXISTENT_FROM_SERVICE_JOURNEY,
      interchange,
      Map.of(),
      Map.of()
    );
  }

  @Test
  void testInterchangeReferringToNonExistentFromServiceJourneyDoesNotThrow() {
    setupTestCaseWithReferenceToNonExistentFromServiceJourney();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      REFERENCE_TO_NON_EXISTENT_FROM_SERVICE_JOURNEY
    );
    Assertions.assertEquals(
      validationReport,
      validator.validate(validationReport)
    );
  }

  private void setupTestCaseWithReferenceToNonExistentToServiceJourney() {
    var nonExistentToJourneyId = "Test:ServiceJourney:999";
    var interchange = createInterchangeWithMaximumWaitTime()
      .withToJourneyRef(
        new VehicleJourneyRefStructure()
          .withRef(ServiceJourneyId.ofValidId(nonExistentToJourneyId).id())
      );

    Map<String, List<LocalDateTime>> activeDates = Map.of(
      fromJourneyId,
      List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0))
    );

    Map<String, ServiceJourneyStop> stops = Map.of(
      fromJourneyId,
      createArrivalStop(fromStopPoint, 14, 0, 0, Optional.empty())
    );

    setupTestData(
      REFERENCE_TO_NON_EXISTENT_TO_SERVICE_JOURNEY,
      interchange,
      activeDates,
      stops
    );
  }

  @Test
  void testInterchangeReferringToNonExistentToServiceJourneyDoesNotThrow() {
    setupTestCaseWithReferenceToNonExistentToServiceJourney();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      REFERENCE_TO_NON_EXISTENT_TO_SERVICE_JOURNEY
    );
    Assertions.assertEquals(
      validationReport,
      validator.validate(validationReport)
    );
  }

  @Test
  void testSatisfiedWaitingTimeGivesNoValidationWarning() {
    setupTestCaseSatisfiedWaitingTime();
    InterchangeWaitingTimeValidator validator =
      new InterchangeWaitingTimeValidator(
        new SimpleValidationEntryFactory(),
        netexDataRepository
      );
    ValidationReport validationReport = new ValidationReport(
      CODESPACE,
      SATISFIED_WAITING_TIME
    );
    ValidationReport resultingReport = validator.validate(validationReport);
    assertTrue(resultingReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testWaitingTimeExceedingWarningTresholdGivesValidationWarning() {
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
  void testMinimumWaitTimeIsNullWhenImpossible() {
    Duration minimum =
      InterchangeWaitingTimeValidator.getShortestActualWaitingTimeForInterchange(
        Stream
          .of(
            LocalDateTime.of(2025, 1, 5, 12, 0, 0),
            LocalDateTime.of(2025, 1, 6, 12, 0, 0),
            LocalDateTime.of(2025, 1, 7, 12, 0, 0),
            LocalDateTime.of(2025, 1, 8, 12, 0, 0)
          )
          .sorted()
          .toList(),
        Stream
          .of(
            LocalDateTime.of(2025, 1, 1, 11, 5, 0),
            LocalDateTime.of(2025, 1, 2, 11, 15, 0),
            LocalDateTime.of(2025, 1, 3, 11, 20, 0),
            LocalDateTime.of(2025, 1, 4, 11, 25, 0)
          )
          .sorted()
          .toList()
      );
    assertEquals(null, minimum);
  }

  @Test
  void testMinimumWaitTime() {
    Duration minimum =
      InterchangeWaitingTimeValidator.getShortestActualWaitingTimeForInterchange(
        Stream
          .of(
            LocalDateTime.of(2025, 1, 1, 11, 0, 0),
            LocalDateTime.of(2025, 1, 2, 11, 0, 0),
            LocalDateTime.of(2025, 1, 3, 11, 0, 0),
            LocalDateTime.of(2025, 1, 4, 11, 0, 0)
          )
          .sorted()
          .toList(),
        Stream
          .of(
            LocalDateTime.of(2025, 1, 2, 11, 0, 0),
            LocalDateTime.of(2025, 1, 3, 11, 0, 0),
            LocalDateTime.of(2025, 1, 4, 11, 0, 0),
            LocalDateTime.of(2025, 1, 5, 11, 0, 0),
            LocalDateTime.of(2025, 1, 6, 11, 0, 0)
          )
          .sorted()
          .toList()
      );
    assertEquals(Duration.ofDays(0), minimum);
  }

  @Test
  void testValidateServiceJourneyInterchangeInfo() {
    var interchangeInfo = ServiceJourneyInterchangeInfo.of(
      "",
      new ServiceJourneyInterchange()
    );
    var fromActiveDates = List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    var toActiveDates = List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0));
    var fromStop = createArrivalStop(1, 23, 45, 0, Optional.empty());
    var toStop = createDepartureStop(2, 0, 15, 0, Optional.empty());

    var validationIssue =
      InterchangeWaitingTimeValidator.validateServiceJourneyInterchangeInfo(
        interchangeInfo,
        fromActiveDates,
        toActiveDates,
        fromStop,
        toStop
      );
    assertTrue(validationIssue == null);
  }

  @Test
  void testValidateServiceJourneyInterchangeInfoLongWaitTimeGivesWarning() {
    var interchangeInfo = ServiceJourneyInterchangeInfo.of(
      "",
      new ServiceJourneyInterchange()
        .withId(serviceJourneyInterchangeId)
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
    );

    var fromActiveDates = List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    var toActiveDates = List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0));
    var fromStop = createArrivalStop(1, 23, 45, 0, Optional.empty());
    var toStop = createDepartureStop(2, 1, 46, 0, Optional.empty());

    var validationIssue =
      InterchangeWaitingTimeValidator.validateServiceJourneyInterchangeInfo(
        interchangeInfo,
        fromActiveDates,
        toActiveDates,
        fromStop,
        toStop
      );
    assertTrue(validationIssue != null);
    assertEquals(
      validationIssue.rule().name(),
      InterchangeWaitingTimeValidator.RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING.name()
    );
    assertEquals(validationIssue.rule().severity(), Severity.WARNING);
  }

  @Test
  void testValidateServiceJourneyWithNoPossibleInterchangeGivesWarning() {
    var interchangeInfo = ServiceJourneyInterchangeInfo.of(
      "",
      new ServiceJourneyInterchange()
        .withId(serviceJourneyInterchangeId)
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
    );

    var fromActiveDates = List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0));
    var toActiveDates = List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
    var fromStop = createArrivalStop(1, 23, 45, 0, Optional.empty());
    var toStop = createDepartureStop(2, 4, 15, 0, Optional.empty());

    var validationIssue =
      InterchangeWaitingTimeValidator.validateServiceJourneyInterchangeInfo(
        interchangeInfo,
        fromActiveDates,
        toActiveDates,
        fromStop,
        toStop
      );
    assertTrue(validationIssue != null);
    assertEquals(
      validationIssue.rule().name(),
      InterchangeWaitingTimeValidator.RULE_NO_INTERCHANGE_POSSIBLE.name()
    );
    assertEquals(validationIssue.rule().severity(), Severity.WARNING);
  }

  @Test
  void testCreateLocalDateTimeFromDayOffsetAndPassingTime() {
    var localDateTime =
      InterchangeWaitingTimeValidator.createLocalDateTimeFromDayOffsetAndPassingTime(
        LocalDateTime.of(2025, 1, 1, 0, 0, 0),
        0,
        LocalTime.of(23, 45, 0)
      );

    assertEquals(LocalDateTime.of(2025, 1, 1, 23, 45, 0), localDateTime);
  }

  @Test
  void testCreateLocalDateTimeFromDayOffsetAndPassingTimeWithDayOffset() {
    var localDateTime =
      InterchangeWaitingTimeValidator.createLocalDateTimeFromDayOffsetAndPassingTime(
        LocalDateTime.of(2025, 1, 1, 0, 0, 0),
        1,
        LocalTime.of(23, 45, 0)
      );

    assertEquals(LocalDateTime.of(2025, 1, 2, 23, 45, 0), localDateTime);
  }

  @Test
  void testSortedLocalDateTimesForServiceJourneyAtStop() {
    List sortedLocalDateTimes = sortedLocalDateTimesForServiceJourneyAtStop(
      List.of(
        LocalDateTime.of(2025, 1, 1, 0, 0, 0),
        LocalDateTime.of(2024, 12, 31, 0, 0, 0)
      ),
      0,
      LocalTime.of(12, 0)
    );
    assertTrue(
      sortedLocalDateTimes
        .get(0)
        .equals(LocalDateTime.of(2024, 12, 31, 12, 0, 0))
    );
    assertTrue(
      sortedLocalDateTimes.get(1).equals(LocalDateTime.of(2025, 1, 1, 12, 0, 0))
    );
  }
}
