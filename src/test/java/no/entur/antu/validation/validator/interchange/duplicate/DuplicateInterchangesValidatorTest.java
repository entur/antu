package no.entur.antu.validation.validator.interchange.duplicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

class DuplicateInterchangesValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      DuplicateInterchangesValidator.class
    );
  }

  @Test
  void testNoDuplicateInterchanges() {
    List<ServiceJourneyInterchange> serviceJourneyInterchanges =
      createServiceJourneyInterchanges(5);

    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addInterchanges(
          serviceJourneyInterchanges.toArray(ServiceJourneyInterchange[]::new)
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testAllDuplicate() {
    List<ServiceJourneyInterchange> serviceJourneyInterchanges =
      createServiceJourneyInterchanges(5, 0, 0, 1, 2, 3, 4);

    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addInterchanges(
          serviceJourneyInterchanges.toArray(ServiceJourneyInterchange[]::new)
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    String message = validationReport
      .getValidationReportEntries()
      .stream()
      .findFirst()
      .map(ValidationReportEntry::getMessage)
      .orElseThrow();

    // assert that 4 distinct interchanges are reported among the 5 identical ones.
    // TODO the validator should report all 5 identical interchanges

    Pattern pattern = Pattern.compile("TST:ServiceJourneyInterchange:[0-4]*");
    assertEquals(
      4,
      pattern
        .matcher(message)
        .results()
        .map(MatchResult::group)
        .distinct()
        .count()
    );
  }

  @Test
  void testSomeDuplicateInterchanges() {
    List<ServiceJourneyInterchange> serviceJourneyInterchanges =
      createServiceJourneyInterchanges(5, 1, 2, 3);

    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addInterchanges(
          serviceJourneyInterchanges.toArray(ServiceJourneyInterchange[]::new)
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    String message = validationReport
      .getValidationReportEntries()
      .stream()
      .findFirst()
      .map(ValidationReportEntry::getMessage)
      .orElseThrow();

    assertThat(
      message,
      CoreMatchers.allOf(
        containsString("TST:ServiceJourneyInterchange:2"),
        containsString("TST:ServiceJourneyInterchange:3")
      )
    );
  }

  @Test
  void testMultiplePairsOfDuplicateInterchanges() {
    List<ServiceJourneyInterchange> serviceJourneyInterchanges1 =
      createServiceJourneyInterchanges(5, 1, 2, 3);
    List<ServiceJourneyInterchange> serviceJourneyInterchanges2 =
      createServiceJourneyInterchanges(5, 6, 7, 8);

    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addInterchanges(
        Stream
          .concat(
            serviceJourneyInterchanges1.stream(),
            serviceJourneyInterchanges2.stream()
          )
          .toArray(ServiceJourneyInterchange[]::new)
      )
      .create();
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(2));

    Pattern duplicatedIds1 = Pattern.compile(
      "TST:ServiceJourneyInterchange:[1-3]*"
    );
    Pattern duplicatedIds2 = Pattern.compile(
      "TST:ServiceJourneyInterchange:[6-8]*"
    );

    // assert that 2 distinct interchanges are reported among the 3 identical ones.
    // TODO the validator should report all 3 identical interchanges
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getMessage)
        .allMatch(message ->
          duplicatedIds1
            .matcher(message)
            .results()
            .map(MatchResult::group)
            .distinct()
            .count() ==
          2 ||
          duplicatedIds2
            .matcher(message)
            .results()
            .map(MatchResult::group)
            .distinct()
            .count() ==
          2
        )
    );
  }

  public List<ServiceJourneyInterchange> createServiceJourneyInterchanges(
    int numberOfServiceJourneyInterchanges
  ) {
    return createServiceJourneyInterchanges(
      numberOfServiceJourneyInterchanges,
      -1
    );
  }

  private List<ServiceJourneyInterchange> createServiceJourneyInterchanges(
    int numberOfServiceJourneyInterchanges,
    int startIndex,
    int... duplicateIndexes
  ) {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

    int maxStartIndex = Math.max(startIndex, 0);

    List<ServiceJourney> serviceJourneys = fragment.createServiceJourneys(
      fragment.journeyPattern().create(),
      (numberOfServiceJourneyInterchanges + maxStartIndex) * 2
    );

    List<Integer> duplicateIndexesList = Arrays
      .stream(duplicateIndexes)
      .boxed()
      .toList();

    List<NetexEntitiesTestFactory.CreateServiceJourneyInterchange> createServiceJourneyInterchanges =
      IntStream
        .range(
          maxStartIndex,
          numberOfServiceJourneyInterchanges + maxStartIndex
        )
        .map(index ->
          duplicateIndexesList.contains(index) ? maxStartIndex : index
        )
        .mapToObj(index ->
          new NetexEntitiesTestFactory.CreateServiceJourneyInterchange()
            .withFromJourneyRef(
              ServiceJourneyId.ofValidId(serviceJourneys.get(index * 2))
            )
            .withToJourneyRef(
              ServiceJourneyId.ofValidId(serviceJourneys.get((index * 2) + 1))
            )
            .withFromPointRef(
              new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1))
            )
            .withToPointRef(
              new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 2))
            )
        )
        .toList();

    return IntStream
      .range(0, createServiceJourneyInterchanges.size())
      .mapToObj(index ->
        createServiceJourneyInterchanges
          .get(index)
          .withId(maxStartIndex + index)
          .create()
      )
      .toList();
  }
}
