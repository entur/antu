package no.entur.antu.validation.validator.interchange.duplicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
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

    NetexTestFragment testData = new NetexTestFragment();
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

    NetexTestFragment testData = new NetexTestFragment();
    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addInterchanges(
          serviceJourneyInterchanges.toArray(ServiceJourneyInterchange[]::new)
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        "Duplicate interchanges found at TST:ServiceJourneyInterchange:1, TST:ServiceJourneyInterchange:2, TST:ServiceJourneyInterchange:3, TST:ServiceJourneyInterchange:4"
      )
    );
  }

  @Test
  void testSomeDuplicateInterchanges() {
    List<ServiceJourneyInterchange> serviceJourneyInterchanges =
      createServiceJourneyInterchanges(5, 1, 2, 3);

    NetexTestFragment testData = new NetexTestFragment();
    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addInterchanges(
          serviceJourneyInterchanges.toArray(ServiceJourneyInterchange[]::new)
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        "Duplicate interchanges found at TST:ServiceJourneyInterchange:2, TST:ServiceJourneyInterchange:3"
      )
    );
  }

  @Test
  void testMultiplePairsOfDuplicateInterchanges() {
    List<ServiceJourneyInterchange> serviceJourneyInterchanges1 =
      createServiceJourneyInterchanges(5, 1, 2, 3);
    List<ServiceJourneyInterchange> serviceJourneyInterchanges2 =
      createServiceJourneyInterchanges(5, 6, 7, 8);

    NetexTestFragment testData = new NetexTestFragment();
    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addInterchanges(
          Stream
            .concat(
              serviceJourneyInterchanges1.stream(),
              serviceJourneyInterchanges2.stream()
            )
            .toArray(ServiceJourneyInterchange[]::new)
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getMessage)
        .allMatch(message ->
          List
            .of(
              "Duplicate interchanges found at TST:ServiceJourneyInterchange:2, TST:ServiceJourneyInterchange:3",
              "Duplicate interchanges found at TST:ServiceJourneyInterchange:7, TST:ServiceJourneyInterchange:8"
            )
            .contains(message)
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
    NetexTestFragment fragment = new NetexTestFragment();

    int maxStartIndex = Math.max(startIndex, 0);

    List<ServiceJourney> serviceJourneys = fragment.createServiceJourneys(
      fragment.journeyPattern().create(),
      (numberOfServiceJourneyInterchanges + maxStartIndex) * 2
    );

    List<Integer> duplicateIndexesList = Arrays
      .stream(duplicateIndexes)
      .boxed()
      .toList();

    List<NetexTestFragment.CreateServiceJourneyInterchange> createServiceJourneyInterchanges =
      IntStream
        .range(
          maxStartIndex,
          numberOfServiceJourneyInterchanges + maxStartIndex
        )
        .map(index ->
          duplicateIndexesList.contains(index) ? maxStartIndex : index
        )
        .mapToObj(index ->
          new NetexTestFragment.CreateServiceJourneyInterchange()
            .withFromJourneyRef(serviceJourneys.get(index * 2).getId())
            .withToJourneyRef(serviceJourneys.get((index * 2) + 1).getId())
            .withFromPointRef("TST:ScheduledStopPoint:" + (index + 1))
            .withToPointRef("TST:ScheduledStopPoint:" + (index + 2))
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
