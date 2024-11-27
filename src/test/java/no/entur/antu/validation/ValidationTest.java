package no.entur.antu.validation;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import no.entur.antu.exception.AntuException;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.DatasetValidator;
import org.entur.netex.validation.validator.SimpleValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

public class ValidationTest {

  private static final String TEST_CODESPACE = "ENT";
  private static final String TEST_LINE_XML_FILE = "line.xml";
  private static final String TEST_COMMON_XML_FILE = "_common.xml";

  private static final String VALIDATION_REPORT_ID = "Test1122";
  private static final String VALIDATION_REPORT_CODEBASE = "TST";
  protected CommonDataRepository commonDataRepositoryMock;
  protected NetexDataRepository netexDataRepositoryMock;
  protected StopPlaceRepository stopPlaceRepositoryMock;
  private final ValidationReportEntryFactory validationReportEntryFactory =
    new SimpleValidationEntryFactory();

  protected ValidationTest() {}

  @BeforeEach
  void resetMocks() {
    this.commonDataRepositoryMock = mock(CommonDataRepository.class);
    Mockito
      .when(commonDataRepositoryMock.hasSharedScheduledStopPoints(anyString()))
      .thenReturn(true);

    this.netexDataRepositoryMock = mock(NetexDataRepository.class);
    this.stopPlaceRepositoryMock = mock(StopPlaceRepository.class);
  }

  protected void mockNoQuayIdsInNetexDataRepository() {
    Mockito
      .when(commonDataRepositoryMock.hasSharedScheduledStopPoints(anyString()))
      .thenReturn(false);
  }

  protected void mockGetStopName(ScheduledStopPointId scheduledStopPointId) {
    QuayId quayId = new QuayId("TST:Quay:007");
    when(
      commonDataRepositoryMock.quayIdForScheduledStopPoint(
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
        commonDataRepositoryMock.quayIdForScheduledStopPoint(
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
        commonDataRepositoryMock.fromToScheduledStopPointIdForServiceLink(
          eq(serviceLinkId),
          anyString()
        )
      )
      .thenReturn(scheduledStopPointIds);
  }

  protected void mockGetServiceJourneyStops(
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops
  ) {
    Mockito
      .when(netexDataRepositoryMock.serviceJourneyStops(anyString()))
      .thenReturn(serviceJourneyStops);
  }

  protected void mockGetServiceJourneyDayTypes(
    Map<ServiceJourneyId, List<DayTypeId>> serviceJourneyDayTypes
  ) {
    Mockito
      .when(netexDataRepositoryMock.serviceJourneyDayTypes(anyString()))
      .thenReturn(serviceJourneyDayTypes);
  }

  protected void mockGetServiceJourneyOperatingDays(
    Map<ServiceJourneyId, List<OperatingDayId>> serviceJourneyOperatingDays
  ) {
    Mockito
      .when(netexDataRepositoryMock.serviceJourneyOperatingDays(anyString()))
      .thenReturn(serviceJourneyOperatingDays);
  }

  protected void mockGetActiveDays(
    Map<ActiveDatesId, ActiveDates> activeDates
  ) {
    Mockito
      .when(netexDataRepositoryMock.activeDates(anyString()))
      .thenReturn(activeDates);
  }

  protected void mockGetServiceJourneyInterchangeInfo(
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos
  ) {
    Mockito
      .when(netexDataRepositoryMock.serviceJourneyInterchangeInfos(anyString()))
      .thenReturn(serviceJourneyInterchangeInfos);
  }

  protected <
    V extends JAXBValidator
  > ValidationReport runValidationOnCommonFile(
    NetexEntitiesIndex netexEntitiesIndex,
    Class<V> validatorClass
  ) {
    return runValidation(netexEntitiesIndex, validatorClass, true);
  }

  protected <V extends DatasetValidator> ValidationReport runDatasetValidation(
    Class<V> validatorClass
  ) {
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

  protected <V extends JAXBValidator> ValidationReport runValidationOnLineFile(
    NetexEntitiesIndex netexEntitiesIndex,
    Class<V> validatorClass
  ) {
    return runValidation(netexEntitiesIndex, validatorClass, false);
  }

  private <V extends JAXBValidator> ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    Class<V> validatorClass,
    boolean mockAsCommonFile
  ) {
    ValidationReport testValidationReport = new ValidationReport(
      VALIDATION_REPORT_CODEBASE,
      VALIDATION_REPORT_ID
    );

    JAXBValidationContext validationContext = new JAXBValidationContext(
      VALIDATION_REPORT_ID,
      netexEntitiesIndex,
      commonDataRepositoryMock,
      v -> stopPlaceRepositoryMock,
      TEST_CODESPACE,
      mockAsCommonFile ? TEST_COMMON_XML_FILE : TEST_LINE_XML_FILE,
      Map.of()
    );

    try {
      V validator = validatorClass.getDeclaredConstructor().newInstance();
      List<ValidationReportEntry> validationReportEntries = validator
        .validate(validationContext)
        .stream()
        .map(validationReportEntryFactory::createValidationReportEntry)
        .toList();
      testValidationReport.addAllValidationReportEntries(
        validationReportEntries
      );

      return testValidationReport;
    } catch (Exception ex) {
      throw new AntuException("Failed to initialize validator", ex);
    }
  }
}
