package no.entur.antu.validation.validator.servicelink.distance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import net.opengis.gml._3.DirectPositionType;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.NetexRefs;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

class UnexpectedDistanceInServiceLinkValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnCommonFile(
      netexEntitiesIndex,
      new UnexpectedDistanceInServiceLinkValidator()
    );
  }

  @Test
  void bothStartAndEndPointsWithinTheLimitsWithPositionListShouldNotReportAnyError() {
    ValidationReport validationReport = runTestWith(
      List.of(
        61.153299,
        10.389718,
        61.153299,
        10.389576,
        61.153328,
        10.388828,
        61.153331,
        10.388738
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void bothStartAndEndPointsWithinTheLimitsWithPointListShouldNotReportAnyError() {
    ValidationReport validationReport = runTestWithDirectPositionType(
      List.of(
        new DirectPositionType().withValue(List.of(61.153299, 10.389718)),
        new DirectPositionType().withValue(List.of(61.153299, 10.389710)),
        new DirectPositionType().withValue(List.of(61.153303, 10.389576)),
        new DirectPositionType().withValue(List.of(61.153328, 10.388828))
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void endPointDistanceOverWarningLimitShouldReportWarning() {
    ValidationReport validationReport = runTestWith(
      List.of(
        61.153299,
        10.389718,
        61.153299,
        10.389576,
        61.153328,
        10.388828,
        61.153642,
        10.388738
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName),
      is(
        Optional.of(
          UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_END_ABOVE_WARNING.name()
        )
      )
    );
  }

  @Test
  void startPointDistanceOverWarningLimitShouldReportWarning() {
    ValidationReport validationReport = runTestWithDirectPositionType(
      List.of(
        new DirectPositionType().withValue(List.of(61.153499, 10.389999)),
        new DirectPositionType().withValue(List.of(61.153299, 10.389710)),
        new DirectPositionType().withValue(List.of(61.153303, 10.389576)),
        new DirectPositionType().withValue(List.of(61.153328, 10.388828))
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName),
      is(
        Optional.of(
          UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_START_ABOVE_WARNING.name()
        )
      )
    );
  }

  @Test
  void endPointDistanceOverMaxLimitShouldReportError() {
    ValidationReport validationReport = runTestWith(
      List.of(
        61.153299,
        10.389718,
        61.153299,
        10.389576,
        61.153328,
        10.388828,
        61.153642,
        10.399738
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName),
      is(
        Optional.of(
          UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_END_ABOVE_LIMIT.name()
        )
      )
    );
  }

  @Test
  void startPointDistanceOverMaxLimitShouldReportAnyError() {
    ValidationReport validationReport = runTestWithDirectPositionType(
      List.of(
        new DirectPositionType().withValue(List.of(61.155499, 10.389999)),
        new DirectPositionType().withValue(List.of(61.153299, 10.389710)),
        new DirectPositionType().withValue(List.of(61.153303, 10.389576)),
        new DirectPositionType().withValue(List.of(61.153328, 10.388828))
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName),
      is(
        Optional.of(
          UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_START_ABOVE_LIMIT.name()
        )
      )
    );
  }

  @Test
  void bothStartAndEndPointDistancesOverWarningLimitShouldReportWarning() {
    ValidationReport validationReport = runTestWith(
      List.of(
        61.153499,
        10.389999,
        61.153299,
        10.389576,
        61.153328,
        10.388828,
        61.153642,
        10.388738
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .anyMatch(entry ->
          entry
            .getName()
            .equals(
              UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_START_ABOVE_WARNING.name()
            )
        )
    );
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .anyMatch(entry ->
          entry
            .getName()
            .equals(
              UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_END_ABOVE_WARNING.name()
            )
        )
    );
  }

  @Test
  void bothStartPointAndStopPointDistancesOverMaxLimitShouldReportError() {
    ValidationReport validationReport = runTestWithDirectPositionType(
      List.of(
        new DirectPositionType().withValue(List.of(61.155499, 10.389999)),
        new DirectPositionType().withValue(List.of(61.153299, 10.389710)),
        new DirectPositionType().withValue(List.of(61.153303, 10.389576)),
        new DirectPositionType().withValue(List.of(61.156328, 10.388828))
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .anyMatch(entry ->
          entry
            .getName()
            .equals(
              UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_START_ABOVE_LIMIT.name()
            )
        )
    );
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .anyMatch(entry ->
          entry
            .getName()
            .equals(
              UnexpectedDistanceInServiceLinkValidator.RULE_DISTANCE_TO_END_ABOVE_LIMIT.name()
            )
        )
    );
  }

  @Test
  void oddNumberOfCoordinatesInLineStringShouldBeIgnored() {
    ValidationReport validationReport = runTestWith(
      List.of(61.153299, 10.389718, 10.388828, 61.153642, 10.399738),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void lessThanFourCoordinatesPointsInLineStringShouldBeIgnored() {
    ValidationReport validationReport = runTestWith(
      List.of(61.153299, 10.389718),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void multipleValidServiceLinksShouldBeValidatedCorrectly() {
    ValidationReport validationReport = runTestWith(
      List.of(
        61.153299,
        10.389718,
        61.153299,
        10.389576,
        61.153328,
        10.388828,
        61.153331,
        10.388738
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void validationShouldBeIgnoredIfThereAreNoQuayCoordinates() {
    ValidationReport validationReport = runTestWith(
      List.of(
        61.153299,
        10.389718,
        61.153299,
        10.389576,
        61.153328,
        10.388828,
        61.153331,
        10.388738
      ),
      null,
      null
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void validationShouldBeIgnoredInCaseOfInvalidLineStringWithLessThenThreeCoordinates() {
    ValidationReport validationReport = runTestWith(
      List.of(61.153299, 10.389718, 61.153299),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void validationShouldBeIgnoredInCaseOfInvalidLineStringWithOddNumberOfCoordinates() {
    ValidationReport validationReport = runTestWith(
      List.of(
        61.153299,
        10.389718,
        61.153299,
        10.389576,
        61.153328,
        10.388828,
        61.153331
      ),
      new QuayCoordinates(10.389718, 61.153299),
      new QuayCoordinates(10.388738, 61.153331)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void datasetWithoutServiceLinksShouldBeIgnored() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport runTestWithDirectPositionType(
    List<DirectPositionType> lineStringCoordinates,
    QuayCoordinates fromQuayCoordinates,
    QuayCoordinates toQuayCoordinates
  ) {
    ScheduledStopPointRefStructure fromStopPointId =
      NetexRefs.scheduledStopPointRef(1);
    ScheduledStopPointRefStructure toStopPointId =
      NetexRefs.scheduledStopPointRef(2);

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    netexEntitiesTestFactory
      .addServiceLink(1, fromStopPointId, toStopPointId)
      .withLineStringPositions(lineStringCoordinates)
      .build();

    withCoordinates(
      ScheduledStopPointId.of(fromStopPointId),
      new QuayId("TST:Quay:1"),
      fromQuayCoordinates
    );
    withCoordinates(
      ScheduledStopPointId.of(toStopPointId),
      new QuayId("TST:Quay:2"),
      toQuayCoordinates
    );

    return runValidation(netexEntitiesTestFactory.build());
  }

  private ValidationReport runTestWith(
    List<Double> lineStringCoordinates,
    QuayCoordinates fromQuayCoordinates,
    QuayCoordinates toQuayCoordinates
  ) {
    ScheduledStopPointRefStructure fromStopPointId =
      NetexRefs.scheduledStopPointRef(1);
    ScheduledStopPointRefStructure toStopPointId =
      NetexRefs.scheduledStopPointRef(2);

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    netexEntitiesTestFactory
      .addServiceLink(1, fromStopPointId, toStopPointId)
      .withLineStringList(lineStringCoordinates)
      .build();

    withCoordinates(
      ScheduledStopPointId.of(fromStopPointId),
      new QuayId("TST:Quay:1"),
      fromQuayCoordinates
    );
    withCoordinates(
      ScheduledStopPointId.of(toStopPointId),
      new QuayId("TST:Quay:2"),
      toQuayCoordinates
    );

    return runValidation(netexEntitiesTestFactory.build());
  }
}
