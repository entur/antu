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
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.NetexRefs;
import no.entur.antu.netex.test.builder.ServiceJourneyBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

class DuplicateInterchangesValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new DuplicateInterchangesValidator()
    );
  }

  @Test
  void testNoDuplicateInterchanges() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    createServiceJourneyInterchanges(netexEntitiesTestFactory, 5);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testAllDuplicate() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    createServiceJourneyInterchanges(
      netexEntitiesTestFactory,
      5,
      0,
      0,
      1,
      2,
      3,
      4
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    String message = validationReport
      .getValidationReportEntries()
      .stream()
      .findFirst()
      .map(ValidationReportEntry::getMessage)
      .orElseThrow();

    // assert that 4 distinct interchanges are reported among the 5 identical ones.

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
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    createServiceJourneyInterchanges(netexEntitiesTestFactory, 5, 1, 2, 3);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    createServiceJourneyInterchanges(netexEntitiesTestFactory, 5, 1, 2, 3);
    createServiceJourneyInterchanges(netexEntitiesTestFactory, 5, 6, 7, 8);

    NetexEntitiesIndex netexEntitiesIndex = netexEntitiesTestFactory.build();

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

  public void createServiceJourneyInterchanges(
    NetexTestData netexEntitiesTestFactory,
    int numberOfServiceJourneyInterchanges
  ) {
    createServiceJourneyInterchanges(
      netexEntitiesTestFactory,
      numberOfServiceJourneyInterchanges,
      -1
    );
  }

  private void createServiceJourneyInterchanges(
    NetexTestData netexEntitiesTestFactory,
    int numberOfServiceJourneyInterchanges,
    int startIndex,
    int... duplicateIndexes
  ) {
    int maxStartIndex = Math.max(startIndex, 0);

    List<ServiceJourneyBuilder> serviceJourneys =
      netexEntitiesTestFactory.addServiceJourneys(
        netexEntitiesTestFactory.addJourneyPattern(),
        (numberOfServiceJourneyInterchanges + maxStartIndex) * 2
      );

    List<Integer> duplicateIndexesList = Arrays
      .stream(duplicateIndexes)
      .boxed()
      .toList();

    IntStream
      .range(maxStartIndex, numberOfServiceJourneyInterchanges + maxStartIndex)
      .forEach(index -> {
        int id = duplicateIndexesList.contains(index) ? maxStartIndex : index;
        netexEntitiesTestFactory
          .addServiceJourneyInterchange(index)
          .withFromJourneyRef(serviceJourneys.get(id * 2).refObject())
          .withToJourneyRef(serviceJourneys.get((id * 2) + 1).refObject())
          .withFromPointRef(NetexRefs.scheduledStopPointRef(id + 1))
          .withToPointRef(NetexRefs.scheduledStopPointRef(id + 2));
      });
  }
}
