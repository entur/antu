package no.entur.antu.validation.validator.servicejourney.transportmode;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.FlexibleLineBuilder;
import no.entur.antu.netex.test.builder.GenericLineBuilder;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.RouteBuilder;
import no.entur.antu.netex.test.builder.ServiceJourneyBuilder;
import no.entur.antu.netex.test.repository.TestCommonDataRepository;
import no.entur.antu.netex.test.repository.TestStopPlaceRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationIssue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TaxiSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

class MismatchedTransportModeSubModeValidatorTest extends ValidatorTestBase {

  private MismatchedTransportModeSubModeValidator validator;
  private NetexTestData netexEntitiesTestFactory;
  private GenericLineBuilder<? extends Line_VersionStructure> line;
  private ServiceJourneyBuilder serviceJourney;

  @BeforeEach
  void setUp() {
    validator = new MismatchedTransportModeSubModeValidator();
    netexEntitiesTestFactory = new NetexTestData();
    line = netexEntitiesTestFactory.addLine(1);
    RouteBuilder route = netexEntitiesTestFactory.addRoute(1);
    JourneyPatternBuilder journeyPattern = netexEntitiesTestFactory
      .addJourneyPattern(1)
      .withRoute(route);
    journeyPattern.addStopPoints(4);
    serviceJourney =
      netexEntitiesTestFactory.addServiceJourney(1, journeyPattern);
  }

  @Test
  void transportModeOnLineMatchesWithStopPlace() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void stopAssignmentsWithValidModeDefinedInLineFileShouldBeConsidered() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    NetexEntitiesIndex netexEntitiesIndex = netexEntitiesTestFactory.build();

    netexEntitiesIndex
      .getQuayIdByStopPointRefIndex()
      .put("TST:ScheduledStopPoint:1", "TST:Quay:1");

    withCommonDataRepository(TestCommonDataRepository.of(0));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesIndex,
      validator
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void stopAssignmentsWithInvalidModeDefinedInLineFileShouldBeConsidered() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.RAIL)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withRailSubmode(RailSubmodeEnumeration.LOCAL)
      );

    NetexEntitiesIndex netexEntitiesIndex = netexEntitiesTestFactory.build();

    netexEntitiesIndex
      .getQuayIdByStopPointRefIndex()
      .put("TST:ScheduledStopPoint:1", "TST:Quay:1");

    withCommonDataRepository(TestCommonDataRepository.of(0));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesIndex,
      validator
    );
    Assertions.assertFalse(validationIssues.isEmpty());
  }

  @Test
  void transportModeOverriddenOnServiceJourneyMatchesWithStopPlace() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.RAIL)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withRailSubmode(RailSubmodeEnumeration.LOCAL)
      );
    serviceJourney
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void railReplacementBusStopsCanBeVisitedByRailReplacementBusService() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(
      TestStopPlaceRepository.ofRailReplacementBusStops(4)
    );

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void railReplacementBusStopsCanOnlyBeVisitedByRailReplacementBusService() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(
      TestStopPlaceRepository.ofRailReplacementBusStops(4)
    );

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertFalse(validationIssues.isEmpty());
  }

  @Test
  void transportModeBusOnServiceJourneyShouldMatchWithTransportModeCoachOnStopPlace() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofNationalCoachStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void transportModeCoachOnServiceJourneyShouldMatchWithTransportModeBusOnStopPlace() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.COACH)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withCoachSubmode(CoachSubmodeEnumeration.NATIONAL_COACH)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void taxiCanStopOnBusStops() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.TAXI)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withTaxiSubmode(TaxiSubmodeEnumeration.CHARTER_TAXI)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void taxiCanStopOnCoachStops() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.TAXI)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withTaxiSubmode(TaxiSubmodeEnumeration.CHARTER_TAXI)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofNationalCoachStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void taxiCannotStopOnStopOtherThanBusOrCoach() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.TAXI)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withTaxiSubmode(TaxiSubmodeEnumeration.CHARTER_TAXI)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalTrainStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertFalse(validationIssues.isEmpty());
  }

  @Test
  void validateOkWhenTransportModeNotFoundOnServiceJourneyNorLine() {
    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void validateOkWhenTransportSubModeNotFoundOnServiceJourneyNorLine() {
    line.withTransportMode(AllVehicleModesOfTransportEnumeration.TAXI);
    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void validateOkWhenTransportModeAndSubModeNotFoundOnQuay() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.TAXI)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withTaxiSubmode(TaxiSubmodeEnumeration.CHARTER_TAXI)
      );
    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(
      TestStopPlaceRepository.ofMissingTransportModeAndSubMode(4)
    );

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void transportModeMissMatchShouldGenerateValidationIssue() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalTrainStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      netexEntitiesTestFactory.build(),
      validator
    );

    Assertions.assertFalse(validationIssues.isEmpty());
  }

  @Test
  void correctTransportModeOnFlexibleLineShouldBeValidated() {
    NetexEntitiesIndex flexNetexEntitiesIndex =
      createFlexNetexEntitiesIndex(createFlexibleLine ->
        createFlexibleLine
          .withFlexibleLineType(FlexibleLineTypeEnumeration.FIXED)
          .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
          .withTransportSubmode(
            new TransportSubmodeStructure()
              .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
          )
      );

    withCommonDataRepository(TestCommonDataRepository.of(4));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(4));

    List<ValidationIssue> validationIssues = validateLineFile(
      flexNetexEntitiesIndex,
      validator
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void incorrectTransportModeOnFlexibleLineShouldBeReported() {
    NetexEntitiesIndex flexNetexEntitiesIndex =
      createFlexNetexEntitiesIndex(createFlexibleLine ->
        createFlexibleLine
          .withTransportMode(AllVehicleModesOfTransportEnumeration.RAIL)
          .withTransportSubmode(
            new TransportSubmodeStructure()
              .withRailSubmode(RailSubmodeEnumeration.LOCAL)
          )
      );

    // create common data and stop place repositories where only the first two stops are mapped to fixed quays
    // (the two following quays can be mapped to flexible areas)
    withCommonDataRepository(TestCommonDataRepository.of(2));
    withStopPlaceRepository(TestStopPlaceRepository.ofLocalBusStops(2));

    List<ValidationIssue> validationIssues = validateLineFile(
      flexNetexEntitiesIndex,
      validator
    );

    Assertions.assertEquals(2, validationIssues.size());
    Assertions.assertTrue(
      validationIssues
        .stream()
        .allMatch(validationIssue ->
          validationIssue
            .rule()
            .equals(
              MismatchedTransportModeSubModeValidator.RULE_INVALID_TRANSPORT_MODE
            )
        )
    );
  }

  /**
   * Create a NetexEntitiesIndex containing a flexible line.
   */
  private NetexEntitiesIndex createFlexNetexEntitiesIndex(
    Consumer<FlexibleLineBuilder> configureFlexibleLine
  ) {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    FlexibleLineBuilder createFlexibleLine = netexEntitiesTestFactory
      .addFlexibleLine()
      .withFlexibleLineType(FlexibleLineTypeEnumeration.MIXED_FLEXIBLE);

    configureFlexibleLine.accept(createFlexibleLine);

    RouteBuilder route = netexEntitiesTestFactory.addRoute();

    JourneyPatternBuilder journeyPattern = netexEntitiesTestFactory
      .addJourneyPattern()
      .withRoute(route);
    journeyPattern.addStopPoints(4);

    netexEntitiesTestFactory.addServiceJourney(journeyPattern);

    return netexEntitiesTestFactory.build();
  }
}
