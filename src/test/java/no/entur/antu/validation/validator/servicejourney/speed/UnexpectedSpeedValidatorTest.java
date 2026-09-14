package no.entur.antu.validation.validator.servicejourney.speed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.NetexRefs;
import no.entur.antu.netex.test.builder.ServiceJourneyBuilder;
import no.entur.antu.netex.test.builder.StopPointInJourneyPatternBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

class UnexpectedSpeedValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new UnexpectedSpeedValidator()
    );
  }

  @Test
  void normalSpeedShouldNotReturnAnyValidationEntry() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.622312, 60.481548),
        new QuayCoordinates(6.632312, 60.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void lowSpeedShouldReturnValidationEntryForLowSpeed() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.612312, 60.471548),
        new QuayCoordinates(6.632312, 60.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .findFirst()
        .orElse(null),
      is(UnexpectedSpeedValidator.RULE_LOW_SPEED.name())
    );
  }

  @Test
  void highSpeedShouldReturnValidationEntryForHighSpeed() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.602312, 60.471548),
        new QuayCoordinates(6.592312, 61.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .findFirst()
        .orElse(null),
      is(UnexpectedSpeedValidator.RULE_HIGH_SPEED.name())
    );
  }

  @Test
  void warningSpeedShouldReturnValidationEntryForHighSpeed() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.602312, 60.471548),
        new QuayCoordinates(6.592312, 60.551548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .findFirst()
        .orElse(null),
      is(UnexpectedSpeedValidator.RULE_WARNING_SPEED.name())
    );
  }

  @Test
  void multipleSpeedViolationShouldBeDetected() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.612312, 60.471548),
        new QuayCoordinates(6.592312, 61.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .toList(),
      is(
        List.of(
          UnexpectedSpeedValidator.RULE_LOW_SPEED.name(),
          UnexpectedSpeedValidator.RULE_HIGH_SPEED.name()
        )
      )
    );
  }

  @Test
  void testPassengerStopAssignmentsInLineFileAndNotOnCommonFileShouldBeOk() {
    List<QuayCoordinates> quayCoordinates = List.of(
      new QuayCoordinates(6.621791, 60.424023),
      new QuayCoordinates(6.612112, 60.471748),
      new QuayCoordinates(6.622312, 60.481548),
      new QuayCoordinates(6.632312, 60.491548)
    );

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();

    netexEntitiesTestFactory
      .addServiceJourney(journeyPatternBuilder)
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    withNoSharedScheduledStopPoints();

    for (int i = 0; i < quayCoordinates.size(); i++) {
      withCoordinates(
        new QuayId("TST:Quay:" + (i + 1)),
        quayCoordinates.get(i)
      );

      netexEntitiesTestFactory
        .addPassengerStopAssignment(i + 1)
        .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(i + 1))
        .withStopPlaceRef(NetexRefs.stopPointRef(i + 1))
        .withQuayRef(NetexRefs.quayRef(i + 1));
    }

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testNoPassengerStopAssignmentsFoundShouldIgnoreValidationGracefully() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();

    ServiceJourneyBuilder serviceJourneyBuilder =
      netexEntitiesTestFactory.addServiceJourney(journeyPatternBuilder);

    serviceJourneyBuilder
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );
    withNoSharedScheduledStopPoints();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private ValidationReport runTestWithQuayCoordinates(
    List<QuayCoordinates> quayCoordinates
  ) {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();
    List<StopPointInJourneyPatternBuilder> stopPointInJourneyPatterns =
      journeyPatternBuilder.addStopPoints(4);

    ServiceJourneyBuilder serviceJourneyBuilder = netexEntitiesTestFactory
      .addServiceJourney(journeyPatternBuilder)
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    IntStream
      .rangeClosed(1, stopPointInJourneyPatterns.size())
      .forEach(index ->
        serviceJourneyBuilder
          .addTimetabledPassingTime(
            index,
            stopPointInJourneyPatterns.get(index - 1)
          )
          .withDepartureTime(LocalTime.of(5, index * 5))
      );

    return runTestWith(quayCoordinates, netexEntitiesTestFactory.build());
  }

  private ValidationReport runTestWith(
    List<QuayCoordinates> quayCoordinates,
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    for (int i = 0; i < quayCoordinates.size(); i++) {
      withCoordinates(
        new ScheduledStopPointId("TST:ScheduledStopPoint:" + (i + 1)),
        new QuayId("TST:Quay:" + (i + 1)),
        quayCoordinates.get(i)
      );
    }

    return runValidation(netexEntitiesIndex);
  }
}
