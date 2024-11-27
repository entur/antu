package no.entur.antu.validation.validator.servicejourney.speed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
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

class UnexpectedSpeedValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      UnexpectedSpeedValidator.class
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

    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    netexEntitiesTestFactory
      .createServiceJourney(createJourneyPattern)
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    mockNoQuayIdsInNetexDataRepository();

    for (int i = 0; i < quayCoordinates.size(); i++) {
      mockGetCoordinates(
        new QuayId("TST:Quay:" + (i + 1)),
        quayCoordinates.get(i)
      );

      netexEntitiesTestFactory
        .createPassengerStopAssignment(i + 1)
        .withScheduledStopPointRef(
          NetexEntitiesTestFactory.createScheduledStopPointRef(i + 1)
        )
        .withStopPlaceRef(NetexEntitiesTestFactory.createStopPointRef(i + 1))
        .withQuayRef(NetexEntitiesTestFactory.createQuayRef(i + 1));
    }

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testNoPassengerStopAssignmentsFoundShouldIgnoreValidationGracefully() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    NetexEntitiesTestFactory.CreateServiceJourney createServiceJourney =
      netexEntitiesTestFactory.createServiceJourney(createJourneyPattern);

    createServiceJourney
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );
    mockNoQuayIdsInNetexDataRepository();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private ValidationReport runTestWithQuayCoordinates(
    List<QuayCoordinates> quayCoordinates
  ) {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();
    List<NetexEntitiesTestFactory.CreateStopPointInJourneyPattern> stopPointInJourneyPatterns =
      createJourneyPattern.createStopPointsInJourneyPattern(4);

    NetexEntitiesTestFactory.CreateServiceJourney createServiceJourney =
      netexEntitiesTestFactory
        .createServiceJourney(createJourneyPattern)
        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
        .withTransportSubmode(
          new TransportSubmodeStructure()
            .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
        );

    IntStream
      .rangeClosed(1, stopPointInJourneyPatterns.size())
      .forEach(index ->
        createServiceJourney
          .createTimetabledPassingTime(
            index,
            stopPointInJourneyPatterns.get(index - 1)
          )
          .withDepartureTime(LocalTime.of(5, index * 5))
      );

    return runTestWith(quayCoordinates, netexEntitiesTestFactory.create());
  }

  private ValidationReport runTestWith(
    List<QuayCoordinates> quayCoordinates,
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    for (int i = 0; i < quayCoordinates.size(); i++) {
      mockGetCoordinates(
        new ScheduledStopPointId("TST:ScheduledStopPoint:" + (i + 1)),
        new QuayId("TST:Quay:" + (i + 1)),
        quayCoordinates.get(i)
      );
    }

    return runValidation(netexEntitiesIndex);
  }
}
