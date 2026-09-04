package no.entur.antu.netex.test;

import java.util.List;
import java.util.Map;
import no.entur.antu.netex.test.repository.TestCommonDataRepository;
import no.entur.antu.netex.test.repository.TestNetexDataRepository;
import no.entur.antu.netex.test.repository.TestStopPlaceRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.DatasetValidator;
import org.entur.netex.validation.validator.SimpleValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.entur.netex.validation.validator.model.ServiceLinkId;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.junit.jupiter.api.BeforeEach;

/**
 * Runs one validator against test data built with {@link NetexTestData}, and gives a test the
 * repository fakes to configure the reference data the validator will ask for.
 *
 * <p>The with* methods put entries in the fakes. Nothing here stubs anything, so a question the
 * validator asks that a test has not answered gets the fake's real answer for absent data rather
 * than a mock's default.
 */
public class ValidatorTestBase {

  protected static final String TEST_CODESPACE = "ENT";
  protected static final String TEST_LINE_XML_FILE = "line.xml";
  protected static final String TEST_COMMON_XML_FILE = "_common.xml";

  /**
   * The report id every fake in this harness is keyed on. One id, so a test that puts data in a
   * fake and a validator that reads it cannot disagree about which report they are in.
   */
  protected static final String VALIDATION_REPORT_ID = "Test1122";
  protected static final String VALIDATION_REPORT_CODEBASE = "TST";

  /**
   * The quay {@link #withStopName} names. Arbitrary, but tests assert on the resulting message.
   */
  private static final QuayId NAMED_STOP_QUAY_ID = new QuayId("TST:Quay:007");
  private static final String NAMED_STOP_NAME = "Test stop name";

  protected TestCommonDataRepository commonDataRepository;
  protected TestNetexDataRepository netexDataRepository;
  protected TestStopPlaceRepository stopPlaceRepository;

  private final ValidationReportEntryFactory validationReportEntryFactory =
    new SimpleValidationEntryFactory();

  protected ValidatorTestBase() {}

  @BeforeEach
  void resetRepositories() {
    this.commonDataRepository = new TestCommonDataRepository();
    this.netexDataRepository = new TestNetexDataRepository();
    this.stopPlaceRepository = new TestStopPlaceRepository();
  }

  /**
   * Replace the common data fake, for a test that wants a pre-populated one.
   */
  protected void withCommonDataRepository(
    TestCommonDataRepository commonDataRepository
  ) {
    this.commonDataRepository = commonDataRepository;
  }

  /**
   * Replace the stop place fake, for a test that wants one of the named fixtures.
   */
  protected void withStopPlaceRepository(
    TestStopPlaceRepository stopPlaceRepository
  ) {
    this.stopPlaceRepository = stopPlaceRepository;
  }

  /**
   * Make the dataset look like one whose shared file carries no scheduled stop points.
   */
  protected void withNoSharedScheduledStopPoints() {
    commonDataRepository.withNoSharedScheduledStopPoints();
  }

  /**
   * Give the scheduled stop point a quay that has a stop place name, so a validator's message can
   * name the stop instead of falling back to its id.
   */
  protected void withStopName(ScheduledStopPointId scheduledStopPointId) {
    commonDataRepository.putQuayId(scheduledStopPointId, NAMED_STOP_QUAY_ID);
    stopPlaceRepository.putStopPlaceName(NAMED_STOP_QUAY_ID, NAMED_STOP_NAME);
  }

  protected void withCoordinates(
    ScheduledStopPointId scheduledStopPointId,
    QuayId quayId,
    QuayCoordinates quayCoordinates
  ) {
    withQuayId(scheduledStopPointId, quayId);
    withCoordinates(quayId, quayCoordinates);
  }

  protected void withQuayId(
    ScheduledStopPointId scheduledStopPointId,
    QuayId quayId
  ) {
    commonDataRepository.putQuayId(scheduledStopPointId, quayId);
  }

  protected void withCoordinates(
    QuayId quayId,
    QuayCoordinates quayCoordinates
  ) {
    stopPlaceRepository.putCoordinates(quayId, quayCoordinates);
  }

  protected void withQuay(StopPlaceId stopPlaceId, QuayId quayId) {
    stopPlaceRepository.putQuay(stopPlaceId, quayId);
  }

  protected void withFromToScheduledStopPointId(
    ServiceLinkId serviceLinkId,
    FromToScheduledStopPointId scheduledStopPointIds
  ) {
    commonDataRepository.putFromToScheduledStopPointId(
      serviceLinkId,
      scheduledStopPointIds
    );
  }

  protected void withServiceJourneyStops(
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops
  ) {
    netexDataRepository.putServiceJourneyStop(
      VALIDATION_REPORT_ID,
      serviceJourneyStops
    );
  }

  protected void withServiceJourneyInterchangeInfos(
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos
  ) {
    serviceJourneyInterchangeInfos.forEach(serviceJourneyInterchangeInfo ->
      netexDataRepository.addServiceJourneyInterchangeInfo(
        VALIDATION_REPORT_ID,
        serviceJourneyInterchangeInfo
      )
    );
  }

  /**
   * Run a JAXB validator against a line file and return its issues, before they are mapped to
   * report entries. Use this when the assertion is about a ValidationRule; a ValidationReport has
   * already collapsed the rule into an entry.
   */
  protected List<ValidationIssue> validateLineFile(
    NetexEntitiesIndex netexEntitiesIndex,
    JAXBValidator validator
  ) {
    return validator.validate(
      jaxbValidationContext(netexEntitiesIndex, TEST_LINE_XML_FILE)
    );
  }

  /**
   * Run a JAXB validator against a common file and return its issues.
   */
  protected List<ValidationIssue> validateCommonFile(
    NetexEntitiesIndex netexEntitiesIndex,
    JAXBValidator validator
  ) {
    return validator.validate(
      jaxbValidationContext(netexEntitiesIndex, TEST_COMMON_XML_FILE)
    );
  }

  /**
   * Run a JAXB validator against a line file and return the resulting validation report.
   */
  protected ValidationReport runValidationOnLineFile(
    NetexEntitiesIndex netexEntitiesIndex,
    JAXBValidator validator
  ) {
    return reportOf(validateLineFile(netexEntitiesIndex, validator));
  }

  /**
   * Run a JAXB validator against a common file and return the resulting validation report.
   */
  protected ValidationReport runValidationOnCommonFile(
    NetexEntitiesIndex netexEntitiesIndex,
    JAXBValidator validator
  ) {
    return reportOf(validateCommonFile(netexEntitiesIndex, validator));
  }

  /**
   * Run a dataset validator, which reads the NeTEx data fake rather than an entities index.
   */
  protected ValidationReport runDatasetValidation(DatasetValidator validator) {
    ValidationReport validationReport = emptyValidationReport();
    validator.validate(validationReport);
    return validationReport;
  }

  protected ValidationReportEntryFactory validationReportEntryFactory() {
    return validationReportEntryFactory;
  }

  private ValidationReport reportOf(List<ValidationIssue> validationIssues) {
    ValidationReport validationReport = emptyValidationReport();
    List<ValidationReportEntry> validationReportEntries = validationIssues
      .stream()
      .map(validationReportEntryFactory::createValidationReportEntry)
      .toList();
    validationReport.addAllValidationReportEntries(validationReportEntries);
    return validationReport;
  }

  private ValidationReport emptyValidationReport() {
    return new ValidationReport(
      VALIDATION_REPORT_CODEBASE,
      VALIDATION_REPORT_ID
    );
  }

  private JAXBValidationContext jaxbValidationContext(
    NetexEntitiesIndex netexEntitiesIndex,
    String filename
  ) {
    return new JAXBValidationContext(
      VALIDATION_REPORT_ID,
      netexEntitiesIndex,
      commonDataRepository,
      codespace -> stopPlaceRepository,
      TEST_CODESPACE,
      filename,
      Map.of()
    );
  }
}
