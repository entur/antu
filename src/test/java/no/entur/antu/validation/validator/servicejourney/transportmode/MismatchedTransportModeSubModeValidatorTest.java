package no.entur.antu.validation.validator.servicejourney.transportmode;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import no.entur.antu.netextestdata.MappingSupport;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.jaxb.CommonDataRepository;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TaxiSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

class MismatchedTransportModeSubModeValidatorTest {

  private static final String TEST_REPORT_ID = "report id";
  private static final String TEST_CODESPACE = "ENT";
  private static final String TEST_FILENAME = "netex.xml";
  private MismatchedTransportModeSubModeValidator validator;
  private NetexEntitiesIndex netexEntitiesIndex;
  private Line line;
  private ServiceJourney serviceJourney;

  @BeforeEach
  void setUp() {
    validator = new MismatchedTransportModeSubModeValidator();
    NetexEntitiesTestFactory testFactory = new NetexEntitiesTestFactory();
    line = testFactory.line().create();
    Route route = testFactory.route().withLine(line).create();
    JourneyPattern journeyPattern = testFactory
      .journeyPattern()
      .withRoute(route)
      .create();
    serviceJourney = testFactory.serviceJourney(line, journeyPattern).create();

    netexEntitiesIndex =
      testFactory
        .netexEntitiesIndex()
        .addLine(line)
        .addRoute(route)
        .addJourneyPatterns(journeyPattern)
        .addServiceJourneys(serviceJourney)
        .create();
  }

  @Test
  void transportModeOnLineMatchesWithStopPlace() {
    line
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
      .withTransportSubmode(
        new TransportSubmodeStructure()
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
      );

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    netexEntitiesIndex
      .getQuayIdByStopPointRefIndex()
      .put("TST:ScheduledStopPoint:1", "TST:Quay:1");

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(0),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    netexEntitiesIndex
      .getQuayIdByStopPointRefIndex()
      .put("TST:ScheduledStopPoint:1", "TST:Quay:1");

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(0),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofRailReplacementBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofRailReplacementBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofNationalCoachStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofNationalCoachStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalTrainStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );

    Assertions.assertFalse(validationIssues.isEmpty());
  }

  @Test
  void validateOkWhenTransportModeNotFoundOnServiceJourneyNorLine() {
    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );
    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void validateOkWhenTransportSubModeNotFoundOnServiceJourneyNorLine() {
    line.withTransportMode(AllVehicleModesOfTransportEnumeration.TAXI);
    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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
    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofMissingTransportModeAndSubMode(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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

    JAXBValidationContext validationContext = createValidationContext(
      netexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalTrainStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );

    Assertions.assertFalse(validationIssues.isEmpty());
  }

  @Test
  void correctTransportModeOnFlexibleLineShouldBeValidated() {
    NetexEntitiesIndex flexNetexEntitiesIndex =
      createFlexNetexEntitiesIndex(flexibleLine ->
        flexibleLine
          .withFlexibleLineType(FlexibleLineTypeEnumeration.FIXED)
          .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
          .withTransportSubmode(
            new TransportSubmodeStructure()
              .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
          )
      );
    JAXBValidationContext validationContext = createValidationContext(
      flexNetexEntitiesIndex,
      TestCommonDataRepository.of(4),
      TestStopPlaceRepository.ofLocalBusStops(4)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
    );

    Assertions.assertTrue(validationIssues.isEmpty());
  }

  @Test
  void incorrectTransportModeOnFlexibleLineShouldBeReported() {
    NetexEntitiesIndex flexNetexEntitiesIndex =
      createFlexNetexEntitiesIndex(flexibleLine ->
        flexibleLine
          .withFlexibleLineType(FlexibleLineTypeEnumeration.MIXED_FLEXIBLE)
          .withTransportMode(AllVehicleModesOfTransportEnumeration.RAIL)
          .withTransportSubmode(
            new TransportSubmodeStructure()
              .withRailSubmode(RailSubmodeEnumeration.LOCAL)
          )
      );

    // create common data and stop place repositories where only the first two stops are mapped to fixed quays
    // (the two following quays can be mapped to flexible areas)
    JAXBValidationContext validationContext = createValidationContext(
      flexNetexEntitiesIndex,
      TestCommonDataRepository.of(2),
      TestStopPlaceRepository.ofLocalBusStops(2)
    );

    List<ValidationIssue> validationIssues = validator.validate(
      validationContext
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
    Function<FlexibleLine, FlexibleLine> configureFlexibleLine
  ) {
    NetexEntitiesTestFactory testFactory = new NetexEntitiesTestFactory();
    FlexibleLine flexibleLine = configureFlexibleLine.apply(
      new FlexibleLine()
        .withId("TST:FlexibleLine:1")
        .withFlexibleLineType(FlexibleLineTypeEnumeration.MIXED_FLEXIBLE)
    );
    JAXBElement<? extends LineRefStructure> lineRef =
      MappingSupport.createJaxbElement(
        new LineRefStructure().withRef(flexibleLine.getId())
      );
    Route route = new Route().withId("TST:Route:1").withLineRef(lineRef);
    JourneyPattern journeyPattern = testFactory
      .journeyPattern()
      .withRoute(route)
      .create();
    JAXBElement<? extends JourneyPatternRefStructure> journeyPatternRef =
      MappingSupport.createJaxbElement(
        new JourneyPatternRefStructure().withRef(journeyPattern.getId())
      );
    ServiceJourney flexServiceJourney = new ServiceJourney()
      .withId("TST:ServiceJourney:1")
      .withLineRef(lineRef)
      .withJourneyPatternRef(journeyPatternRef);

    NetexEntitiesIndex flexNetexEntitiesIndex = new NetexEntitiesIndexImpl();
    flexNetexEntitiesIndex
      .getFlexibleLineIndex()
      .put(flexibleLine.getId(), flexibleLine);
    flexNetexEntitiesIndex.getRouteIndex().put(route.getId(), route);
    flexNetexEntitiesIndex
      .getJourneyPatternIndex()
      .put(journeyPattern.getId(), journeyPattern);
    flexNetexEntitiesIndex
      .getServiceJourneyIndex()
      .put(flexServiceJourney.getId(), flexServiceJourney);

    return flexNetexEntitiesIndex;
  }

  private static JAXBValidationContext createValidationContext(
    NetexEntitiesIndex netexEntitiesIndex,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new JAXBValidationContext(
      TEST_REPORT_ID,
      netexEntitiesIndex,
      commonDataRepository,
      n -> stopPlaceRepository,
      TEST_CODESPACE,
      TEST_FILENAME,
      Map.of()
    );
  }
}
