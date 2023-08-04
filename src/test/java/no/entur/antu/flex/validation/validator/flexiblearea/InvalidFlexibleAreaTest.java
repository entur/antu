package no.entur.antu.flex.validation.validator.flexiblearea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;

class InvalidFlexibleAreaTest {

  @Test
  void testDataSetWithoutFlexibleStopPlacesShouldBeIgnoredGracefully() {
    NetexTestFragment testData = new NetexTestFragment();

    ValidationReport validationReport = setupAndRunValidation(
      testData.netexEntitiesIndex().create()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testLineStringWithValidLinearRing() {
    ValidationReport validationReport = runTestWithGivenCoordinates(
      List.of(
        63.13858931533193,
        9.928943287130938,
        63.126880322529765,
        9.924811717421225,
        63.11070486401104,
        9.963012144500766,
        63.0899891971636,
        9.983576501128129,
        63.07437578651208,
        10.019824646870518,
        63.08307660166303,
        10.051222872093758,
        63.09826061189036,
        10.04922025521767,
        63.09929352506772,
        10.063209726376083,
        63.09769946405965,
        10.074523158554369,
        63.12506609202889,
        10.120711232810317,
        63.13858931533193,
        9.928943287130938
      )
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testLineStringWithNonClosedLineStringShouldFail() {
    ValidationReport validationReport = runTestWithGivenCoordinates(
      List.of(
        63.13858931533193,
        9.928943287130938,
        63.126880322529765,
        9.924811717421225,
        63.11070486401104,
        9.963012144500766,
        63.0899891971636,
        9.983576501128129,
        63.07437578651208,
        10.019824646870518,
        63.08307660166303,
        10.051222872093758
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testLineStringWithTooFewCoordinatesShouldFail() {
    ValidationReport validationReport = runTestWithGivenCoordinates(
      List.of(
        63.13858931533193,
        9.928943287130938,
        63.126880322529765,
        9.924811717421225
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testSelfInteractingLinearRingShouldFail() {
    ValidationReport validationReport = runTestWithGivenCoordinates(
      List.of(
        63.13858931533193,
        9.928943287130938,
        63.126880322529765,
        9.924811717421225,
        63.11070486401104,
        9.963012144500766,
        63.0899891971636,
        9.983576501128129,
        63.07437578651208,
        10.019824646870518,
        63.08307660166303,
        10.051222872093758,
        63.09826061189036,
        10.04922025521767,
        63.09929352506772,
        10.063209726376083,
        63.09769946405965,
        10.074523158554369,
        63.12506609202889,
        10.120711232810317,
        63.11070486401104,
        9.963012144500766,
        63.09769946405965,
        10.074523158554369,
        63.13858931533193,
        9.928943287130938
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testIncompleteCoordinatesShouldFail() {
    ValidationReport validationReport = runTestWithGivenCoordinates(
      List.of(
        63.13858931533193,
        9.928943287130938,
        63.126880322529765,
        9.924811717421225,
        63.11070486401104,
        9.963012144500766,
        63.0899891971636,
        9.983576501128129,
        63.07437578651208,
        10.019824646870518,
        63.08307660166303,
        10.051222872093758,
        63.09826061189036,
        10.04922025521767,
        63.09929352506772,
        63.09769946405965,
        10.074523158554369,
        63.12506609202889,
        10.120711232810317
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testMissingCoordinatesShouldIgnoreValidationGracefully() {
    ValidationReport validationReport = runTestWithGivenCoordinates(List.of());

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingFlexibleStopAreaShouldIgnoreValidationGracefully() {
    NetexTestFragment testData = new NetexTestFragment();

    FlexibleStopPlace flexibleStopPlace =
      new NetexTestFragment.CreateFlexibleStopPlace().create();

    ValidationReport validationReport = setupAndRunValidation(
      testData.netexEntitiesIndex(flexibleStopPlace).create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingPolygonShouldIgnoreValidationGracefully2() {
    NetexTestFragment testData = new NetexTestFragment();

    FlexibleArea flexibleArea = testData.flexibleArea().create();

    FlexibleStopPlace flexibleStopPlace = testData
      .flexibleStopPlace(flexibleArea.withPolygon(null))
      .create();

    ValidationReport validationReport = setupAndRunValidation(
      testData.netexEntitiesIndex(flexibleStopPlace).create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private static ValidationReport runTestWithGivenCoordinates(
    List<Double> coordinates
  ) {
    NetexTestFragment testData = new NetexTestFragment();

    FlexibleArea flexibleArea = testData
      .flexibleArea()
      .withCoordinates(coordinates)
      .create();

    FlexibleStopPlace flexibleStopPlace = testData
      .flexibleStopPlace(flexibleArea)
      .create();

    return setupAndRunValidation(
      testData.netexEntitiesIndex(flexibleStopPlace).create()
    );
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    InvalidFlexibleArea invalidFlexibleArea = new InvalidFlexibleArea(
        (code, message, dataLocation) ->
      new ValidationReportEntry(
        message,
        code,
        ValidationReportEntrySeverity.ERROR
      )
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

    invalidFlexibleArea.validate(testValidationReport, validationContext);

    return testValidationReport;
  }
}
