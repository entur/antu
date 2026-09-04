package no.entur.antu.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetexValidationProfileTest {

  private static final String LINE_FILE = "netex.xml";
  private static final String COMMON_FILE = "_shared_data.xml";
  private static final String SUPPORTED_VERSION_CONTENT =
    "<PublicationDelivery version=\"1.15:NO-NeTEx-networktimetable:1.5\"></PublicationDelivery>";
  private static final String UNSUPPORTED_VERSION_CONTENT =
    "<PublicationDelivery version=\"2:NO-NeTEx-networktimetable:2\"></PublicationDelivery>";

  private NetexValidatorsRunner netexValidatorsRunner;
  private ValidationStateRepository validationStateRepository;
  private ValidationState validationState;
  private NetexValidationProfile netexValidationProfile;

  @BeforeEach
  void setUp() {
    netexValidatorsRunner = mock(NetexValidatorsRunner.class);
    validationStateRepository = mock(ValidationStateRepository.class);
    validationState = new ValidationState();
    // A state that is present is what marks the validation as still running.
    when(validationStateRepository.getValidationState(anyString()))
      .thenReturn(validationState);
    netexValidationProfile =
      new NetexValidationProfile(
        Map.of(ValidationProfile.TIMETABLE, netexValidatorsRunner),
        validationStateRepository,
        false,
        false,
        new NetexProfileVersionValidator()
      );
  }

  @Test
  void aFileWithASupportedVersionIsDelegatedToTheRunner() {
    byte[] fileContent = SUPPORTED_VERSION_CONTENT.getBytes();
    ValidationReport expectedReport = new ValidationReport("tst", "reportId");
    when(
      netexValidatorsRunner.validate(
        eq("tst"),
        eq("reportId"),
        eq(LINE_FILE),
        eq(fileContent),
        anyBoolean(),
        anyBoolean(),
        any()
      )
    )
      .thenReturn(expectedReport);

    assertSame(expectedReport, validate(LINE_FILE, fileContent));
  }

  @Test
  void aFileWithAnUnsupportedVersionIsRejectedWithoutRunningTheValidators() {
    ValidationReport report = validate(
      LINE_FILE,
      UNSUPPORTED_VERSION_CONTENT.getBytes()
    );

    assertTrue(report.hasError());
    assertEquals(
      List.of("UNSUPPORTED_NETEX_VERSION"),
      report
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .toList()
    );
    verify(netexValidatorsRunner, never())
      .validate(any(), any(), any(), any(), anyBoolean(), anyBoolean(), any());
  }

  /**
   * Rejecting a common file rejects the shared data the line files resolve their references
   * against, so they have to be told to skip the NeTEx validators. Nothing runs the validators
   * runner on this path, so its completion callback cannot be what records this.
   */
  @Test
  void aRejectedCommonFileMarksTheValidationAsFailedInACommonFile() {
    validate(COMMON_FILE, UNSUPPORTED_VERSION_CONTENT.getBytes());

    assertTrue(validationState.hasErrorInCommonFile());
    verify(validationStateRepository)
      .updateValidationState(eq("reportId"), any());
  }

  @Test
  void aRejectedLineFileLeavesTheOtherFilesAlone() {
    validate(LINE_FILE, UNSUPPORTED_VERSION_CONTENT.getBytes());

    assertFalse(validationState.hasErrorInCommonFile());
    verify(validationStateRepository, never())
      .updateValidationState(any(), any());
  }

  /**
   * A missing state means the validation already reached a terminal status. A redelivered job then
   * has nothing to add, so it must not report a rejection against a report that is already
   * published.
   */
  @Test
  void anAlreadyCompletedValidationRejectsNothing() {
    when(validationStateRepository.getValidationState(anyString()))
      .thenReturn(null);
    byte[] fileContent = UNSUPPORTED_VERSION_CONTENT.getBytes();
    ValidationReport emptyReport = new ValidationReport("tst", "reportId");
    when(
      netexValidatorsRunner.validate(
        any(),
        any(),
        any(),
        any(),
        anyBoolean(),
        anyBoolean(),
        any()
      )
    )
      .thenReturn(emptyReport);

    assertSame(emptyReport, validate(COMMON_FILE, fileContent));
    assertFalse(validationState.hasErrorInCommonFile());
  }

  private ValidationReport validate(String filename, byte[] fileContent) {
    return netexValidationProfile.validate(
      "timetable",
      "tst",
      "reportId",
      filename,
      fileContent,
      mock(NetexValidationProgressCallBack.class)
    );
  }
}
