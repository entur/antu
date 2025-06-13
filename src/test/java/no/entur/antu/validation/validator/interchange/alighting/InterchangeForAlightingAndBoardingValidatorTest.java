package no.entur.antu.validation.validator.interchange.alighting;

import static no.entur.antu.validation.validator.interchange.alighting.InterchangeForAlightingAndBoardingValidator.ALIGHTING_RULE;
import static no.entur.antu.validation.validator.interchange.alighting.InterchangeForAlightingAndBoardingValidator.BOARDING_RULE;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import no.entur.antu.common.repository.TestNetexDataRepository;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.validation.validator.SimpleValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

class InterchangeForAlightingAndBoardingValidatorTest {

  private TestNetexDataRepository netexDataRepository;
  private InterchangeForAlightingAndBoardingValidator validator;

  private String serviceJourneyInterchangeId = "ServiceJourneyInterchange";
  private ServiceJourneyId fromServiceJourneyId = ServiceJourneyId.ofValidId(
    "Test:ServiceJourney:1"
  );
  private ServiceJourneyId toServiceJourneyId = ServiceJourneyId.ofValidId(
    "Test:ServiceJourney:2"
  );
  private ValidationReport initialValidationReport;
  private String validationReportId = "id";

  private int fromStopPointId = 1;
  private int toStopPointId = 2;

  @BeforeEach
  void setUp() {
    this.netexDataRepository = new TestNetexDataRepository();
    this.validator =
      new InterchangeForAlightingAndBoardingValidator(
        netexDataRepository,
        new SimpleValidationEntryFactory()
      );
    this.initialValidationReport =
      new ValidationReport("tst", validationReportId);
    this.netexDataRepository.addServiceJourneyInterchangeInfo(
        initialValidationReport.getValidationReportId(),
        createServiceJourneyInterchangeInfo()
      );
  }

  private ServiceJourneyStop createServiceJourneyStop(
    int stopPointId,
    Boolean isForAlighting,
    Boolean isForBoarding
  ) {
    ScheduledStopPointId stopId = ScheduledStopPointId.of(
      createStopPointRef(stopPointId)
    );

    TimetabledPassingTime passingTime = new TimetabledPassingTime()
      .withArrivalTime(LocalTime.of(0, 0, 0));
    return ServiceJourneyStop.of(
      stopId,
      passingTime,
      isForAlighting,
      isForBoarding
    );
  }

  private ServiceJourneyStop createFromPointStop(
    Boolean isForAlighting,
    Boolean isForBoarding
  ) {
    return createServiceJourneyStop(
      fromStopPointId,
      isForAlighting,
      isForBoarding
    );
  }

  private ServiceJourneyStop createToPointStop(
    Boolean isForAlighting,
    Boolean isForBoarding
  ) {
    return createServiceJourneyStop(
      toStopPointId,
      isForAlighting,
      isForBoarding
    );
  }

  private ScheduledStopPointRefStructure createStopPointRef(int stopPointId) {
    return new ScheduledStopPointRefStructure()
      .withRef(
        NetexEntitiesTestFactory
          .createScheduledStopPointRef(stopPointId)
          .getRef()
      );
  }

  private ServiceJourneyInterchangeInfo createServiceJourneyInterchangeInfo() {
    return ServiceJourneyInterchangeInfo.of(
      "",
      new ServiceJourneyInterchange()
        .withId(serviceJourneyInterchangeId)
        .withFromJourneyRef(
          new VehicleJourneyRefStructure().withRef(fromServiceJourneyId.id())
        )
        .withToJourneyRef(
          new VehicleJourneyRefStructure().withRef(toServiceJourneyId.id())
        )
        .withFromPointRef(createStopPointRef(fromStopPointId))
        .withToPointRef(createStopPointRef(toStopPointId))
    );
  }

  @Test
  void testValidationWhenAlightingAndBoardingValuesAreValid() {
    ServiceJourneyStop fromStopPoint = createFromPointStop(true, false);
    ServiceJourneyStop toStopPoint = createToPointStop(false, true);

    this.netexDataRepository.putServiceJourneyStop(
        validationReportId,
        Map.of(
          fromServiceJourneyId,
          List.of(fromStopPoint),
          toServiceJourneyId,
          List.of(toStopPoint)
        )
      );

    ValidationReport validationReport = validator.validate(
      initialValidationReport
    );
    Assertions.assertEquals(
      0,
      validationReport.getValidationReportEntries().size()
    );
  }

  @Test
  void testValidationWhenAlightingIsDisallowedOnAlightingStop() {
    ServiceJourneyStop fromStopPoint = createFromPointStop(false, false);
    ServiceJourneyStop toStopPoint = createToPointStop(false, true);

    this.netexDataRepository.putServiceJourneyStop(
        validationReportId,
        Map.of(
          fromServiceJourneyId,
          List.of(fromStopPoint),
          toServiceJourneyId,
          List.of(toStopPoint)
        )
      );

    ValidationReport validationReport = validator.validate(
      initialValidationReport
    );
    Assertions.assertEquals(
      1,
      validationReport.getValidationReportEntries().size()
    );
    Assertions.assertEquals(
      1,
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .get(ALIGHTING_RULE.name())
    );
  }

  @Test
  void testValidationWhenBoardingIsDisallowedOnBoardingStop() {
    ServiceJourneyStop fromStopPoint = createFromPointStop(true, false);
    ServiceJourneyStop toStopPoint = createToPointStop(false, false);

    this.netexDataRepository.putServiceJourneyStop(
        validationReportId,
        Map.of(
          fromServiceJourneyId,
          List.of(fromStopPoint),
          toServiceJourneyId,
          List.of(toStopPoint)
        )
      );

    ValidationReport validationReport = validator.validate(
      initialValidationReport
    );
    Assertions.assertEquals(
      1,
      validationReport.getValidationReportEntries().size()
    );
    Assertions.assertEquals(
      1,
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .get(BOARDING_RULE.name())
    );
  }

  @Test
  void testValidationWhenBoardingAndAlightingAreNull() {
    ServiceJourneyStop fromStopPoint = createFromPointStop(null, null);
    ServiceJourneyStop toStopPoint = createToPointStop(null, null);

    this.netexDataRepository.putServiceJourneyStop(
        validationReportId,
        Map.of(
          fromServiceJourneyId,
          List.of(fromStopPoint),
          toServiceJourneyId,
          List.of(toStopPoint)
        )
      );

    ValidationReport validationReport = validator.validate(
      initialValidationReport
    );
    Assertions.assertEquals(
      0,
      validationReport.getValidationReportEntries().size()
    );
  }
}
