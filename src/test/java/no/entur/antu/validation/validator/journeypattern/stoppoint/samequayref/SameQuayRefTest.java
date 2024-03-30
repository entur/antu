package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.JourneyPattern;

class SameQuayRefTest {

  @Test
  void testNoSameQuayRefOnStopPoints() {
    NetexTestFragment testFragment = new NetexTestFragment();

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(1)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(1)
            .withScheduledStopPointId(1)
            .create(),
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(2)
            .withScheduledStopPointId(2)
            .create()
        )
      )
      .create();

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    ScheduledStopPointId fromStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId toStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
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

    ValidationReport validationReport = setupAndRunValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .create(),
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testSameQuayRefOnStopPoints() {
    NetexTestFragment testFragment = new NetexTestFragment();

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(1)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(1)
            .withScheduledStopPointId(1)
            .create(),
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(2)
            .withScheduledStopPointId(2)
            .create()
        )
      )
      .create();

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    ScheduledStopPointId fromStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId toStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    QuayId testQuayId1 = new QuayId("TST:Quay:1");

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
      .thenReturn(testQuayId1);

    ValidationReport validationReport = setupAndRunValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .create(),
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    CommonDataRepository commonDataRepository
  ) {
    SameQuayRef sameStopsInJourneyPatterns = new SameQuayRef(
      (code, message, dataLocation) ->
        new ValidationReportEntry(
          message,
          code,
          ValidationReportEntrySeverity.ERROR
        ),
      commonDataRepository
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

    sameStopsInJourneyPatterns.validate(
      testValidationReport,
      validationContext
    );

    return testValidationReport;
  }
}
