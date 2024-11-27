package no.entur.antu.netexdata.collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.junit.jupiter.api.Test;

class DatedServiceJourneysCollectorTest {

  @Test
  void testOneDatedServiceJourneyPerServiceJourney() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    IntStream
      .rangeClosed(1, 3)
      .forEach(i ->
        netexEntitiesTestFactory.createDatedServiceJourney(
          i,
          netexEntitiesTestFactory.createServiceJourney(
            i,
            netexEntitiesTestFactory.createJourneyPattern(i)
          ),
          netexEntitiesTestFactory
            .createServiceCalendarFrame()
            .createOperatingDay(i, LocalDate.of(2024, 1, 1))
        )
      );

    Map<String, String> operatingDaysPerServiceJourney =
      DatedServiceJourneysCollector.getOperatingDaysPerServiceJourneyIsAsStrings(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(3, operatingDaysPerServiceJourney.size());
  }

  @Test
  void testMultipleDatedServiceJourneysPerServiceJourney() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceJourney serviceJourney1 =
      netexEntitiesTestFactory.createServiceJourney(
        1,
        netexEntitiesTestFactory.createJourneyPattern(1)
      );
    IntStream
      .rangeClosed(1, 3)
      .forEach(i ->
        netexEntitiesTestFactory.createDatedServiceJourney(
          i,
          serviceJourney1,
          netexEntitiesTestFactory
            .createServiceCalendarFrame()
            .createOperatingDay(i, LocalDate.of(2024, 1, 1 + i))
        )
      );

    NetexEntitiesTestFactory.CreateServiceJourney serviceJourney2 =
      netexEntitiesTestFactory.createServiceJourney(
        2,
        netexEntitiesTestFactory.createJourneyPattern(2)
      );
    IntStream
      .rangeClosed(4, 5)
      .forEach(i ->
        netexEntitiesTestFactory.createDatedServiceJourney(
          i,
          serviceJourney2,
          netexEntitiesTestFactory
            .createServiceCalendarFrame()
            .createOperatingDay(i, LocalDate.of(2024, 2, 1 + i))
        )
      );

    Map<String, String> operatingDaysPerServiceJourney =
      DatedServiceJourneysCollector.getOperatingDaysPerServiceJourneyIsAsStrings(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(2, operatingDaysPerServiceJourney.size());
    assertEquals(
      3,
      operatingDaysPerServiceJourney
        .get(serviceJourney1.ref())
        .split(",")
        .length
    );
    assertEquals(
      2,
      operatingDaysPerServiceJourney
        .get(serviceJourney2.ref())
        .split(",")
        .length
    );
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
