package no.entur.antu.validation.flex.validator.flexiblearea;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;

class InvalidFlexibleAreaValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnCommonFile(
      netexEntitiesIndex,
      InvalidFlexibleAreaValidator.class
    );
  }

  @Test
  void testDataSetWithoutFlexibleStopPlacesShouldBeIgnoredGracefully() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
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
  void testMissingCoordinatesShouldFail() {
    ValidationReport validationReport = runTestWithGivenCoordinates(List.of());

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testMissingFlexibleStopAreaShouldIgnoreValidationGracefully() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    netexEntitiesTestFactory.createFlexibleStopPlace();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingPolygonShouldIgnoreValidationGracefully2() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    netexEntitiesTestFactory
      .createFlexibleStopPlace()
      .flexibleArea(1)
      .withNullPolygon(true);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private ValidationReport runTestWithGivenCoordinates(
    List<Double> coordinates
  ) {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    netexEntitiesTestFactory
      .createFlexibleStopPlace()
      .flexibleArea(1)
      .withCoordinates(coordinates);

    return runValidation(netexEntitiesTestFactory.create());
  }
}
