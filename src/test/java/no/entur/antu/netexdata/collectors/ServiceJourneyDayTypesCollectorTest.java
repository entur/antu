package no.entur.antu.netexdata.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

class ServiceJourneyDayTypesCollectorTest {

  @Test
  void testValidServiceJourneysWithDayTypes() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    NetexEntitiesTestFactory.CreateJourneyPattern journeyPattern =
      netexEntitiesTestFactory.createJourneyPattern();
    List<NetexEntitiesTestFactory.CreateStopPointInJourneyPattern> stopPointsInJourneyPattern =
      journeyPattern.createStopPointsInJourneyPattern(4);

    List<NetexEntitiesTestFactory.CreateServiceJourney> serviceJourneys =
      netexEntitiesTestFactory.createServiceJourneys(journeyPattern, 4);

    serviceJourneys.forEach(serviceJourney -> {
      serviceJourney.createTimetabledPassingTimes(stopPointsInJourneyPattern);
      serviceJourney.addDayTypeRefs(
        netexEntitiesTestFactory
          .createServiceCalendarFrame()
          .createDayTypes(
            4,
            DayOfWeekEnumeration.MONDAY,
            DayOfWeekEnumeration.TUESDAY
          )
      );
    });

    Map<String, String> dayTypesPerServiceJourneyId =
      ServiceJourneyDayTypesCollector.getDayTypesPerServiceJourneyIdAsStrings(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(4, dayTypesPerServiceJourneyId.size());
    dayTypesPerServiceJourneyId.forEach((serviceJourneyId, dayTypes) ->
      assertEquals(4, dayTypes.split(",").length)
    );
  }

  @Test
  void testInValidServiceJourneysFilteredOut() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    NetexEntitiesTestFactory.CreateJourneyPattern journeyPattern =
      netexEntitiesTestFactory.createJourneyPattern();
    List<NetexEntitiesTestFactory.CreateStopPointInJourneyPattern> stopPointsInJourneyPattern =
      journeyPattern.createStopPointsInJourneyPattern(4);

    List<NetexEntitiesTestFactory.CreateServiceJourney> serviceJourneys =
      netexEntitiesTestFactory.createServiceJourneys(journeyPattern, 4);

    IntStream
      .range(0, serviceJourneys.size() - 1)
      .filter(i -> i % 2 == 0)
      .mapToObj(serviceJourneys::get)
      .forEach(serviceJourney -> {
        serviceJourney.createTimetabledPassingTimes(stopPointsInJourneyPattern);
        serviceJourney.addDayTypeRefs(
          netexEntitiesTestFactory
            .createServiceCalendarFrame()
            .createDayTypes(
              4,
              DayOfWeekEnumeration.MONDAY,
              DayOfWeekEnumeration.TUESDAY
            )
        );
      });

    Map<String, String> dayTypesPerServiceJourneyId =
      ServiceJourneyDayTypesCollector.getDayTypesPerServiceJourneyIdAsStrings(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(2, dayTypesPerServiceJourneyId.size());
  }

  private static JAXBValidationContext createContext(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return new JAXBValidationContext(
      "test123",
      netexEntitiesIndex,
      null,
      null,
      "TST",
      "fileSchemaVersion",
      null
    );
  }
}
