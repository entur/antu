package no.entur.antu.validation.validator.servicejourney.passingtime;

import static no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeValidator.*;

import java.util.List;
import no.entur.antu.common.netex.NetexTestDataSample;
import no.entur.antu.common.netex.NetexTestEnvironment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

class NonIncreasingTimetabledPassingTimeValidatorTest extends ValidationTest {

  private boolean containsValidationIssuesForRule(
    List<ValidationIssue> validationIssues,
    ValidationRule rule
  ) {
    List<ValidationIssue> validationIssuesForRule = validationIssues
      .stream()
      .filter(issue -> issue.rule() == rule)
      .toList();
    return validationIssuesForRule.size() > 0;
  }

  private NetexTestEnvironment buildNetexTestEnvironment(
    ServiceJourney serviceJourney,
    JourneyPattern journeyPattern
  ) {
    NetexTestEnvironment netexTestEnvironment = new NetexTestEnvironment();
    return netexTestEnvironment
      .toBuilder()
      .addServiceJourney(serviceJourney)
      .addJourneyPattern(journeyPattern)
      .build();
  }

  private NetexTestEnvironment addFlexStopPlaceFromSharedFileToNetexTestEnvironment(
    NetexTestEnvironment netexTestEnvironment,
    FlexibleStopPlace flexibleStopPlace
  ) {
    return netexTestEnvironment
      .toBuilder()
      .addFlexibleStopPlaceToCommonDataRepository(
        "RUT:ScheduledStopPoint:1",
        flexibleStopPlace
      )
      .build();
  }

  private NetexTestEnvironment addFlexStopPlaceFromLineFileToNetexTestEnvironment(
    NetexTestEnvironment netexTestEnvironment,
    FlexibleStopPlace flexibleStopPlace
  ) {
    return netexTestEnvironment
      .toBuilder()
      .addFlexibleStopPlaceToNetexEntitiesIndex(
        "RUT:ScheduledStopPoint:1",
        flexibleStopPlace
      )
      .build();
  }

  @Test
  void testValidateServiceJourneyWithCompleteStopTimes() {
    ServiceJourney serviceJourney = NetexTestDataSample.defaultServiceJourney();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    NetexTestEnvironment netexTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();
    List<ValidationIssue> issues = validator.validate(
      netexTestEnvironment.getJaxbValidationContext()
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(issues, RULE_INCOMPLETE_TIME)
    );
  }

  @Test
  void testValidateServiceJourneyWithIncompleteStopTimesForRegularStop() {
    ServiceJourney serviceJourney =
      NetexTestDataSample.serviceJourneyWithIncompletePassingTimesForRegularStop();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    NetexTestEnvironment netexTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();
    List<ValidationIssue> issues = validator.validate(
      netexTestEnvironment.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(issues, RULE_INCOMPLETE_TIME)
    );
  }

  @Test
  void testValidateServiceJourneyWithInconsistentStopTimesForRegularStop() {
    ServiceJourney serviceJourney =
      NetexTestDataSample.serviceJourneyWithInconsistentPassingTimesForRegularStop();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    NetexTestEnvironment netexTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();
    List<ValidationIssue> issues = validator.validate(
      netexTestEnvironment.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(issues, RULE_INCONSISTENT_TIME)
    );
  }

  @Test
  void testValidateServiceJourneyWithFlexibleStop() {
    ServiceJourney serviceJourney =
      NetexTestDataSample.serviceJourneyWithFlexibleTimetabledPassingTime();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    FlexibleStopPlace flexibleStopPlace =
      NetexTestDataSample.defaultFlexibleStopPlace();

    NetexTestEnvironment baseTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();

    NetexTestEnvironment envWithFlexibleStopFromSharedFile =
      addFlexStopPlaceFromSharedFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );
    List<ValidationIssue> issuesWhenUsingSharedFile = validator.validate(
      envWithFlexibleStopFromSharedFile.getJaxbValidationContext()
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_INCOMPLETE_TIME
      )
    );

    NetexTestEnvironment envWithFlexibleStopFromLineFile =
      addFlexStopPlaceFromLineFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );
    List<ValidationIssue> issuesWhenUsingLineFile = validator.validate(
      envWithFlexibleStopFromLineFile.getJaxbValidationContext()
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_INCOMPLETE_TIME
      )
    );
  }

  @Test
  void testValidateServiceJourneyWithIncompletePassingTimeForFlexibleStop() {
    ServiceJourney serviceJourney =
      NetexTestDataSample.serviceJourneyWithIncompletePassingTimeForFlexibleStop();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    FlexibleStopPlace flexibleStopPlace =
      NetexTestDataSample.defaultFlexibleStopPlace();

    NetexTestEnvironment baseTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );
    NetexTestEnvironment envWithFlexibleStopFromSharedFile =
      addFlexStopPlaceFromSharedFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );
    NetexTestEnvironment envWithFlexibleStopFromLineFile =
      addFlexStopPlaceFromLineFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();

    List<ValidationIssue> issuesWhenUsingSharedFile = validator.validate(
      envWithFlexibleStopFromSharedFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_INCOMPLETE_TIME
      )
    );

    List<ValidationIssue> issuesWhenUsingLineFile = validator.validate(
      envWithFlexibleStopFromLineFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_INCOMPLETE_TIME
      )
    );
  }

  @Test
  void testValidateServiceJourneyWithInconsistentPassingTimeForFlexibleStop() {
    ServiceJourney serviceJourney =
      NetexTestDataSample.serviceJourneyWithInconsistentPassingTimeForFlexibleStop();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    FlexibleStopPlace flexibleStopPlace =
      NetexTestDataSample.defaultFlexibleStopPlace();

    NetexTestEnvironment baseTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );
    NetexTestEnvironment envWithFlexibleStopFromSharedFile =
      addFlexStopPlaceFromSharedFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );
    NetexTestEnvironment envWithFlexibleStopFromLineFile =
      addFlexStopPlaceFromLineFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();
    List<ValidationIssue> issuesWhenUsingSharedFile = validator.validate(
      envWithFlexibleStopFromSharedFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_INCONSISTENT_TIME
      )
    );

    List<ValidationIssue> issuesWhenUsingLineFile = validator.validate(
      envWithFlexibleStopFromLineFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_INCONSISTENT_TIME
      )
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByRegularStopNonIncreasingTime() {
    ServiceJourney serviceJourney = NetexTestDataSample.defaultServiceJourney();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();

    TimetabledPassingTime firstPassingTime =
      NetexTestDataSample.getFirstPassingTime(serviceJourney);
    TimetabledPassingTime secondPassingTime =
      NetexTestDataSample.getSecondPassingTime(serviceJourney);
    secondPassingTime.withArrivalTime(
      firstPassingTime.getDepartureTime().minusMinutes(1)
    );

    NetexTestEnvironment netexTestEnvironment = new NetexTestEnvironment()
      .toBuilder()
      .addServiceJourney(serviceJourney)
      .addJourneyPattern(journeyPattern)
      .build();

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();
    List<ValidationIssue> issues = validator.validate(
      netexTestEnvironment.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(issues, RULE_NON_INCREASING_TIME)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStop() {
    ServiceJourney serviceJourney = NetexTestDataSample.defaultServiceJourney();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    FlexibleStopPlace flexibleStopPlace =
      NetexTestDataSample.defaultFlexibleStopPlace();

    NetexTestEnvironment baseTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );
    NetexTestEnvironment envWithFlexibleStopFromSharedFile =
      addFlexStopPlaceFromSharedFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );
    NetexTestEnvironment envWithFlexibleStopFromLineFile =
      addFlexStopPlaceFromLineFileToNetexTestEnvironment(
        baseTestEnvironment,
        flexibleStopPlace
      );

    TimetabledPassingTime firstPassingTime =
      NetexTestDataSample.getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();
    List<ValidationIssue> issuesWhenUsingSharedFile = validator.validate(
      envWithFlexibleStopFromSharedFile.getJaxbValidationContext()
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_INCOMPLETE_TIME
      )
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_NON_INCREASING_TIME
      )
    );

    List<ValidationIssue> issuesWhenUsingLineFile = validator.validate(
      envWithFlexibleStopFromLineFile.getJaxbValidationContext()
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_INCOMPLETE_TIME
      )
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_NON_INCREASING_TIME
      )
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopArea() {
    ServiceJourney serviceJourney = NetexTestDataSample.defaultServiceJourney();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    FlexibleStopPlace flexibleStopPlace =
      NetexTestDataSample.defaultFlexibleStopPlace();

    NetexTestEnvironment baseTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );
    NetexTestEnvironment envWithFlexibleStopsFromSharedFile =
      baseTestEnvironment
        .toBuilder()
        .addFlexibleStopPlaceToCommonDataRepository(
          "RUT:ScheduledStopPoint:1",
          flexibleStopPlace
        )
        .addFlexibleStopPlaceToCommonDataRepository(
          "RUT:ScheduledStopPoint:2",
          flexibleStopPlace
        )
        .build();
    NetexTestEnvironment envWithFlexibleStopsFromLineFile = baseTestEnvironment
      .toBuilder()
      .addFlexibleStopPlaceToNetexEntitiesIndex(
        "RUT:ScheduledStopPoint:1",
        flexibleStopPlace
      )
      .addFlexibleStopPlaceToNetexEntitiesIndex(
        "RUT:ScheduledStopPoint:2",
        flexibleStopPlace
      )
      .build();

    TimetabledPassingTime firstPassingTime =
      NetexTestDataSample.getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    TimetabledPassingTime secondPassingTime =
      NetexTestDataSample.getSecondPassingTime(serviceJourney);
    secondPassingTime
      .withLatestArrivalTime(secondPassingTime.getDepartureTime())
      .withEarliestDepartureTime(
        secondPassingTime.getDepartureTime().plusMinutes(1)
      )
      .withArrivalTime(null)
      .withDepartureTime(null);

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();

    List<ValidationIssue> issuesWhenUsingSharedFile = validator.validate(
      envWithFlexibleStopsFromSharedFile.getJaxbValidationContext()
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_INCOMPLETE_TIME
      )
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_NON_INCREASING_TIME
      )
    );

    List<ValidationIssue> issuesWhenUsingLineFile = validator.validate(
      envWithFlexibleStopsFromLineFile.getJaxbValidationContext()
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_INCOMPLETE_TIME
      )
    );
    Assertions.assertFalse(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_NON_INCREASING_TIME
      )
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopAreaNonIncreasingTime() {
    ServiceJourney serviceJourney = NetexTestDataSample.defaultServiceJourney();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    FlexibleStopPlace flexibleStopPlace =
      NetexTestDataSample.defaultFlexibleStopPlace();

    NetexTestEnvironment netexTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );
    NetexTestEnvironment envWithFlexibleStopsFromSharedFile =
      netexTestEnvironment
        .toBuilder()
        .addFlexibleStopPlaceToCommonDataRepository(
          "RUT:ScheduledStopPoint:1",
          flexibleStopPlace
        )
        .addFlexibleStopPlaceToCommonDataRepository(
          "RUT:ScheduledStopPoint:2",
          flexibleStopPlace
        )
        .build();
    NetexTestEnvironment envWithFlexibleStopsFromLineFile = netexTestEnvironment
      .toBuilder()
      .addFlexibleStopPlaceToNetexEntitiesIndex(
        "RUT:ScheduledStopPoint:1",
        flexibleStopPlace
      )
      .addFlexibleStopPlaceToNetexEntitiesIndex(
        "RUT:ScheduledStopPoint:2",
        flexibleStopPlace
      )
      .build();

    TimetabledPassingTime firstPassingTime =
      NetexTestDataSample.getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);
    TimetabledPassingTime secondPassingTime =
      NetexTestDataSample.getSecondPassingTime(serviceJourney);
    secondPassingTime
      .withEarliestDepartureTime(
        firstPassingTime.getEarliestDepartureTime().minusMinutes(1)
      )
      .withLatestArrivalTime(
        secondPassingTime.getEarliestDepartureTime().plusMinutes(1)
      )
      .withArrivalTime(null)
      .withDepartureTime(null);

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();

    List<ValidationIssue> issuesWhenUsingSharedFile = validator.validate(
      envWithFlexibleStopsFromSharedFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_NON_INCREASING_TIME
      )
    );

    List<ValidationIssue> issuesWhenUsingLineFile = validator.validate(
      envWithFlexibleStopsFromLineFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingLineFile,
        RULE_NON_INCREASING_TIME
      )
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStopNonIncreasingTime() {
    ServiceJourney serviceJourney = NetexTestDataSample.defaultServiceJourney();
    JourneyPattern journeyPattern = NetexTestDataSample.defaultJourneyPattern();
    FlexibleStopPlace flexibleStopPlace =
      NetexTestDataSample.defaultFlexibleStopPlace();

    NetexTestEnvironment netexTestEnvironment = buildNetexTestEnvironment(
      serviceJourney,
      journeyPattern
    );
    NetexTestEnvironment envWithFlexibleStopsFromSharedFile =
      netexTestEnvironment
        .toBuilder()
        .addFlexibleStopPlaceToCommonDataRepository(
          "RUT:ScheduledStopPoint:1",
          flexibleStopPlace
        )
        .build();
    NetexTestEnvironment envWithFlexibleStopsFromLineFile = netexTestEnvironment
      .toBuilder()
      .addFlexibleStopPlaceToNetexEntitiesIndex(
        "RUT:ScheduledStopPoint:1",
        flexibleStopPlace
      )
      .build();

    TimetabledPassingTime firstPassingTime =
      NetexTestDataSample.getFirstPassingTime(serviceJourney);
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);
    TimetabledPassingTime secondPassingTime =
      NetexTestDataSample.getSecondPassingTime(serviceJourney);
    secondPassingTime
      .withArrivalTime(firstPassingTime.getLatestArrivalTime().minusMinutes(1))
      .withDepartureTime(null);

    NonIncreasingPassingTimeValidator validator =
      new NonIncreasingPassingTimeValidator();

    List<ValidationIssue> issuesWhenUsingSharedFile = validator.validate(
      envWithFlexibleStopsFromSharedFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingSharedFile,
        RULE_NON_INCREASING_TIME
      )
    );

    List<ValidationIssue> issuesWhenUsingLinefile = validator.validate(
      envWithFlexibleStopsFromLineFile.getJaxbValidationContext()
    );
    Assertions.assertTrue(
      containsValidationIssuesForRule(
        issuesWhenUsingLinefile,
        RULE_NON_INCREASING_TIME
      )
    );
  }
}
