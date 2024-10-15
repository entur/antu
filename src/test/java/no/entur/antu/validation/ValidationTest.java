package no.entur.antu.validation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import no.entur.antu.exception.AntuException;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.DatasetValidator;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

public class ValidationTest {

  private static final String VALIDATION_REPORT_ID = "Test1122";
  private static final String VALIDATION_REPORT_CODEBASE = "TST";
  protected NetexDataRepository netexDataRepositoryMock;
  protected StopPlaceRepository stopPlaceRepositoryMock;

  protected ValidationTest() {}

  @BeforeEach
  void resetMocks() {
    this.netexDataRepositoryMock = mock(NetexDataRepository.class);
    Mockito
      .when(netexDataRepositoryMock.hasQuayIds(anyString()))
      .thenReturn(true);
    Mockito
      .when(
        netexDataRepositoryMock.hasServiceJourneyInterchangeInfos(anyString())
      )
      .thenReturn(true);

    this.stopPlaceRepositoryMock = mock(StopPlaceRepository.class);
  }

  protected void mockNoQuayIdsInNetexDataRepository() {
    Mockito
      .when(netexDataRepositoryMock.hasQuayIds(anyString()))
      .thenReturn(false);
  }

  protected void mockGetStopName(ScheduledStopPointId scheduledStopPointId) {
    QuayId quayId = new QuayId("TST:Quay:007");
    when(
      netexDataRepositoryMock.quayIdForScheduledStopPoint(
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
        netexDataRepositoryMock.quayIdForScheduledStopPoint(
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

  protected void mockGetFromToScheduledStopPointId(
    ServiceLinkId serviceLinkId,
    FromToScheduledStopPointId scheduledStopPointIds
  ) {
    Mockito
      .when(
        netexDataRepositoryMock.fromToScheduledStopPointIdForServiceLink(
          eq(serviceLinkId),
          anyString()
        )
      )
      .thenReturn(scheduledStopPointIds);
  }

  protected void mockGetServiceJourneyStops(
    ServiceJourneyId serviceJourneyId,
    List<ServiceJourneyStop> serviceJourneyStops
  ) {
    Mockito
      .when(
        netexDataRepositoryMock.serviceJourneyStops(
          anyString(),
          eq(serviceJourneyId)
        )
      )
      .thenReturn(serviceJourneyStops);
  }

  protected void mockGetServiceJourneyInterchangeInfo(
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos
  ) {
    Mockito
      .when(netexDataRepositoryMock.serviceJourneyInterchangeInfos(anyString()))
      .thenReturn(serviceJourneyInterchangeInfos);
  }

  protected <
    V extends AntuNetexValidator
  > ValidationReport runValidationOnCommonFile(
    NetexEntitiesIndex netexEntitiesIndex,
    Class<V> validatorClass
  ) {
    return runValidation(netexEntitiesIndex, validatorClass, true);
  }

  protected <V extends DatasetValidator> ValidationReport runDatasetValidation(
    Class<V> validatorClass
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

    try {
      V validator = validatorClass
        .getDeclaredConstructor(
          ValidationReportEntryFactory.class,
          NetexDataRepository.class
        )
        .newInstance(validationReportEntryFactory, netexDataRepositoryMock);
      validator.validate(testValidationReport);

      return testValidationReport;
    } catch (Exception ex) {
      throw new AntuException("Failed to initialize validator", ex);
    }
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

    JAXBValidationContext validationContext = mock(JAXBValidationContext.class);
    when(validationContext.isCommonFile()).thenReturn(mockAsCommonFile);

    when(validationContext.getValidationReportId())
      .thenReturn(VALIDATION_REPORT_ID);
    when(validationContext.getNetexEntitiesIndex())
      .thenReturn(netexEntitiesIndex);
    when(validationContext.getNetexDataRepository())
      .thenReturn(netexDataRepositoryMock);
    when(validationContext.getStopPlaceRepository())
      .thenReturn(stopPlaceRepositoryMock);

    try {
      V validator = validatorClass
        .getDeclaredConstructor(ValidationReportEntryFactory.class)
        .newInstance(validationReportEntryFactory);
      validator.validate(testValidationReport, validationContext);

      return testValidationReport;
    } catch (Exception ex) {
      throw new AntuException("Failed to initialize validator", ex);
    }
  }
}
