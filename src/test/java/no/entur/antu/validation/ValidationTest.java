package no.entur.antu.validation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.ScheduledStopPointIds;
import no.entur.antu.model.ServiceLinkId;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

public class ValidationTest {

  private static final String VALIDATION_REPORT_ID = "Test1122";
  private static final String VALIDATION_REPORT_CODEBASE = "TST";
  protected CommonDataRepository commonDataRepositoryMock;
  protected StopPlaceRepository stopPlaceRepositoryMock;

  protected ValidationTest() {}

  @BeforeEach
  void resetMocks() {
    this.commonDataRepositoryMock = mock(CommonDataRepository.class);
    Mockito
      .when(commonDataRepositoryMock.hasQuayIds(anyString()))
      .thenReturn(true);

    this.stopPlaceRepositoryMock = mock(StopPlaceRepository.class);
  }

  protected void mockNoQuayIdsInCommonDataRepository() {
    Mockito
      .when(commonDataRepositoryMock.hasQuayIds(anyString()))
      .thenReturn(false);
  }

  protected void mockGetStopName(ScheduledStopPointId scheduledStopPointId) {
    QuayId quayId = new QuayId("TST:Quay:007");
    when(
      commonDataRepositoryMock.findQuayIdForScheduledStopPoint(
        scheduledStopPointId,
        VALIDATION_REPORT_ID
      )
    )
      .thenReturn(quayId);
    when(stopPlaceRepositoryMock.getStopPlaceNameForQuayId(quayId))
      .thenReturn("Test stop name");
  }

  protected void mockGetCoordinates(
    ScheduledStopPointId scheduledStopPointId,
    QuayId quayId,
    QuayCoordinates quayCoordinates
  ) {
    mockGetQuayId(scheduledStopPointId, quayId);
    mockGetCoordinates(quayId, quayCoordinates);
  }

  protected void mockGetQuayId(
    ScheduledStopPointId scheduledStopPointId,
    QuayId quayId
  ) {
    Mockito
      .when(
        commonDataRepositoryMock.findQuayIdForScheduledStopPoint(
          eq(scheduledStopPointId),
          anyString()
        )
      )
      .thenReturn(quayId);
  }

  protected void mockGetCoordinates(
    QuayId quayId,
    QuayCoordinates quayCoordinates
  ) {
    Mockito
      .when(stopPlaceRepositoryMock.getCoordinatesForQuayId(quayId))
      .thenReturn(quayCoordinates);
  }

  protected void mockGetScheduledStopPointIds(
    ServiceLinkId serviceLinkId,
    ScheduledStopPointIds scheduledStopPointIds
  ) {
    Mockito
      .when(
        commonDataRepositoryMock.findScheduledStopPointIdsForServiceLink(
          eq(serviceLinkId),
          anyString()
        )
      )
      .thenReturn(scheduledStopPointIds);
  }

  protected <
    V extends AntuNetexValidator
  > ValidationReport runValidationOnCommonFile(
    NetexEntitiesIndex netexEntitiesIndex,
    Class<V> validatorClass
  ) {
    return runValidation(netexEntitiesIndex, validatorClass, true);
  }

  protected <
    V extends AntuNetexValidator
  > ValidationReport runValidationOnLineFile(
    NetexEntitiesIndex netexEntitiesIndex,
    Class<V> validatorClass
  ) {
    return runValidation(netexEntitiesIndex, validatorClass, false);
  }

  private <V extends AntuNetexValidator> ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    Class<V> validatorClass,
    boolean mockAsCommonFile
  ) {
    ValidationReportEntryFactory validationReportEntryFactory = (
        code,
        message,
        dataLocation
      ) ->
      new ValidationReportEntry(
        message,
        code,
        ValidationReportEntrySeverity.ERROR
      );

    ValidationReport testValidationReport = new ValidationReport(
      VALIDATION_REPORT_CODEBASE,
      VALIDATION_REPORT_ID
    );

    ValidationContextWithNetexEntitiesIndex validationContext = mock(
      ValidationContextWithNetexEntitiesIndex.class
    );
    when(validationContext.getNetexEntitiesIndex())
      .thenReturn(netexEntitiesIndex);

    when(validationContext.isCommonFile()).thenReturn(mockAsCommonFile);

    try {
      V validator = validatorClass
        .getDeclaredConstructor(
          ValidationReportEntryFactory.class,
          CommonDataRepository.class,
          StopPlaceRepository.class
        )
        .newInstance(
          validationReportEntryFactory,
          commonDataRepositoryMock,
          stopPlaceRepositoryMock
        );
      validator.validate(testValidationReport, validationContext);

      return testValidationReport;
    } catch (Exception ex) {
      throw new AntuException("Failed to initialize validator", ex);
    }
  }
}
