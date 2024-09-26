package no.entur.antu.validation.validator.servicejourney.transportmode;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.TransportModeAndSubMode;
import no.entur.antu.model.TransportSubMode;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

class MismatchedTransportModeValidatorIntegrationTest {

  public static final String TEST_FILE_MISSING_TRANSPORT_MODE =
    "NWY_Line_8600_20240131_Missing_transport_mode.xml";

  public static final String TEST_FILE_NO_COMPOSITE_FRAME =
    "ENT_No_Composite_Frame.xml";

  /**
   * TransportMode on Line is mandatory. This is expected to be validated before this validation rule,
   * the validation entry for the Missing transport mode, will already be created.
   * So this validation rule should simply ignore the validation if there is no transportMode exists.
   * So we will return true to ignore the validation.
   */
  @Test
  void testFile() throws IOException {
    ValidationReport validationReport = getValidationReport(
      TEST_FILE_MISSING_TRANSPORT_MODE,
      "NWY"
    );
    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testNoCompositeFrame() throws IOException {
    ValidationReport validationReport = getValidationReport(
      TEST_FILE_NO_COMPOSITE_FRAME,
      "ENT"
    );
    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport getValidationReport(
    String testFile,
    String codeSpace
  ) throws IOException {
    ValidationReport testValidationReport = new ValidationReport(
      codeSpace,
      "Test1122"
    );

    try (
      InputStream testDatasetAsStream = getClass()
        .getResourceAsStream('/' + testFile)
    ) {
      assert testDatasetAsStream != null;

      ValidationContext validationContext = mock(ValidationContext.class);

      NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));
      when(validationContext.getNetexXMLParser()).thenReturn(netexXMLParser);

      when(validationContext.getXmlNode())
        .thenReturn(
          netexXMLParser.parseByteArrayToXdmNode(
            testDatasetAsStream.readAllBytes()
          )
        );

      CommonDataRepository commonDataRepository = mock(
        CommonDataRepository.class
      );

      when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);
      QuayId testQuayId = new QuayId(codeSpace + ":Quay:1234");

      when(commonDataRepository.quayIdForScheduledStopPoint(any(), anyString()))
        .thenReturn(testQuayId);

      StopPlaceRepository stopPlaceRepository = mock(StopPlaceRepository.class);

      when(stopPlaceRepository.getTransportModesForQuayId(testQuayId))
        .thenReturn(
          new TransportModeAndSubMode(
            AllVehicleModesOfTransportEnumeration.COACH,
            new TransportSubMode("someCoachSubMode")
          )
        );

      MismatchedTransportModeValidator mismatchedTransportModeValidator =
        new MismatchedTransportModeValidator(
          (code, message, dataLocation) ->
            new ValidationReportEntry(
              message,
              code,
              ValidationReportEntrySeverity.ERROR
            ),
          commonDataRepository,
          stopPlaceRepository
        );

      mismatchedTransportModeValidator.validate(
        testValidationReport,
        validationContext
      );
    }

    return testValidationReport;
  }
}
