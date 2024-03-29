package no.entur.antu.validation.validator.servicelink;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import net.opengis.gml._3.DirectPositionType;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.ServiceLink;

class InvalidServiceLinkTest {

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
          InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT.name()
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
          InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT.name()
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
          InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT.name()
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
          InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT.name()
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
              InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT.name()
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
              InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT.name()
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
              InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT.name()
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
              InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT.name()
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
    NetexTestFragment testData = new NetexTestFragment();

    ValidationReport validationReport = setupAndRunValidation(
      testData.netexEntitiesIndex().create(),
      null,
      null
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private static ValidationReport runTestWithDirectPositionType(
    List<DirectPositionType> lineStringCoordinates,
    QuayCoordinates fromQuayCoordinates,
    QuayCoordinates toQuayCoordinates
  ) {
    ScheduledStopPointId fromStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId toStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    NetexTestFragment testData = new NetexTestFragment();
    ServiceLink serviceLink = testData
      .serviceLink(fromStopPointId.id(), toStopPointId.id())
      .withLineStringPositions(lineStringCoordinates)
      .create();

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );
    StopPlaceRepository stopPlaceRepository = Mockito.mock(
      StopPlaceRepository.class
    );

    QuayId testQuayId1 = new QuayId("TST:Quay:1");
    QuayId testQuayId2 = new QuayId("TST:Quay:2");

    Mockito.when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);
    Mockito
      .when(
        commonDataRepository.findQuayIdForScheduledStopPoint(
          eq(fromStopPointId),
          anyString()
        )
      )
      .thenReturn(testQuayId1);
    Mockito
      .when(
        commonDataRepository.findQuayIdForScheduledStopPoint(
          eq(toStopPointId),
          anyString()
        )
      )
      .thenReturn(testQuayId2);

    Mockito
      .when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId1))
      .thenReturn(fromQuayCoordinates);

    Mockito
      .when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId2))
      .thenReturn(toQuayCoordinates);

    return setupAndRunValidation(
      testData.netexEntitiesIndex(serviceLink).create(),
      commonDataRepository,
      stopPlaceRepository
    );
  }

  private static ValidationReport runTestWith(
    List<Double> lineStringCoordinates,
    QuayCoordinates fromQuayCoordinates,
    QuayCoordinates toQuayCoordinates
  ) {
    ScheduledStopPointId fromStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId toStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    NetexTestFragment testData = new NetexTestFragment();
    ServiceLink serviceLink = testData
      .serviceLink(fromStopPointId.id(), toStopPointId.id())
      .withLineStringList(lineStringCoordinates)
      .create();

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );
    StopPlaceRepository stopPlaceRepository = Mockito.mock(
      StopPlaceRepository.class
    );

    QuayId testQuayId1 = new QuayId("TST:Quay:1");
    QuayId testQuayId2 = new QuayId("TST:Quay:2");

    Mockito.when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);
    Mockito
      .when(
        commonDataRepository.findQuayIdForScheduledStopPoint(
          eq(fromStopPointId),
          anyString()
        )
      )
      .thenReturn(testQuayId1);
    Mockito
      .when(
        commonDataRepository.findQuayIdForScheduledStopPoint(
          eq(toStopPointId),
          anyString()
        )
      )
      .thenReturn(testQuayId2);

    Mockito
      .when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId1))
      .thenReturn(fromQuayCoordinates);

    Mockito
      .when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId2))
      .thenReturn(toQuayCoordinates);

    return setupAndRunValidation(
      testData.netexEntitiesIndex(serviceLink).create(),
      commonDataRepository,
      stopPlaceRepository
    );
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    InvalidServiceLinks invalidServiceLinks = new InvalidServiceLinks(
      (code, message, dataLocation) ->
        new ValidationReportEntry(
          message,
          code,
          ValidationReportEntrySeverity.ERROR
        ),
      commonDataRepository,
      stopPlaceRepository
    );

    ValidationReport testValidationReport = new ValidationReport(
      "TST",
      "Test1122"
    );

    ValidationContextWithNetexEntitiesIndex validationContext = mock(
      ValidationContextWithNetexEntitiesIndex.class
    );
    when(validationContext.getNetexEntitiesIndex())
      .thenReturn(netexEntitiesIndex);
    when(validationContext.isCommonFile()).thenReturn(true);

    invalidServiceLinks.validate(testValidationReport, validationContext);

    return testValidationReport;
  }
}
