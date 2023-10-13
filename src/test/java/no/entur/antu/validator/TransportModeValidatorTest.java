package no.entur.antu.validator;

import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stop.model.QuayId;
import no.entur.antu.stop.model.StopPlaceTransportModes;
import no.entur.antu.stop.model.TransportSubMode;
import no.entur.antu.validator.transportmodevalidator.TransportModeValidator;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransportModeValidatorTest {

    private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser();

    private static class TestData {

        enum LineType {Line, FlexibleLine}

        private static final String CODE_SPACE = "TST";
        private static final String VALIDATION_REPORT_ID = "tst12345";
        private static final QuayId QUAY_ID = new QuayId("TST:Quay:1234");
        private static final String SCHEDULED_STOP_POINT_REF = "RUT:ScheduledStopPoint:default-11605";
        private static final String SERVICE_JOURNEY_REF = "RUT:ServiceJourney:1-173790-25022298";
        private static final String TRANSPORT_MODE_IN_LINE_TEMPLATE = "${transportModeInLine}";
        private static final String TRANSPORT_MODE_IN_SERVICE_JOURNEY_TEMPLATE = "${transportModeInServiceJourney}";
        private static final String FLEXIBLE_LINE_TYPE_TEMPLATE = "${flexibleLineType}";
        private static final String LINE_TYPE_TEMPLATE = "${lineType}";

        private LineType lineType = LineType.Line;
        private String flexibleLineTypeBlock = "";
        private String transportModeBlockInLine = "";
        private String transportModeBlockInServiceJourney = "";

        public String getData() {
            String data = """
                    <PublicationDelivery xmlns="http://www.netex.org.uk/netex"
                                         version="1.04:NO-NeTEx-networktimetable:1.0">
                      <dataObjects>
                        <CompositeFrame id="RUT:CompositeFrame:1" version="any" modification="new">
                          <frames>
                            <ServiceFrame id="RUT:ServiceFrame:1" version="2023-01-26-065417">
                              <routes>
                                <Route id="RUT:Route:1-227" version="2023-01-26-065417">
                                  <${lineType}Ref ref="RUT:${lineType}:1" version="any"/>
                                </Route>
                              </routes>
                              <lines>
                                <${lineType} id="RUT:${lineType}:1" version="any">
                                  ${transportModeInLine}
                                  ${flexibleLineType}
                                </${lineType}>
                              </lines>
                              <journeyPatterns>
                                <JourneyPattern id="RUT:JourneyPattern:1-227" version="2023-01-26-065417">
                                  <RouteRef ref="RUT:Route:1-227" version="2023-01-26-065417"/>
                                  <pointsInSequence>
                                    <StopPointInJourneyPattern id="RUT:StopPointInJourneyPattern:1-227-1" version="2023-01-26-065417" order="1">
                                      <ScheduledStopPointRef ref="RUT:ScheduledStopPoint:default-11605"/>
                                      <ForAlighting>false</ForAlighting>
                                      <DestinationDisplayRef ref="RUT:DestinationDisplay:8015"/>
                                    </StopPointInJourneyPattern>
                                    <StopPointInJourneyPattern id="RUT:StopPointInJourneyPattern:1-227-20" version="2023-01-26-065417" order="20">
                                      <ScheduledStopPointRef ref="RUT:ScheduledStopPoint:default-11134"/>
                                      <ForBoarding>false</ForBoarding>
                                    </StopPointInJourneyPattern>
                                  </pointsInSequence>
                                </JourneyPattern>
                              </journeyPatterns>
                            </ServiceFrame>
                            <TimetableFrame id="RUT:TimetableFrame:1" version="2023-01-26-065417">
                              <vehicleJourneys>
                                <ServiceJourney id="RUT:ServiceJourney:1-173790-25022298" version="2023-01-26-065417">
                                  <JourneyPatternRef ref="RUT:JourneyPattern:1-227" version="2023-01-26-065417"/>
                                  ${transportModeInServiceJourney}
                                </ServiceJourney>
                              </vehicleJourneys>
                            </TimetableFrame>
                          </frames>
                        </CompositeFrame>
                      </dataObjects>
                    </PublicationDelivery>
                    """;
            return data
                    .replace(LINE_TYPE_TEMPLATE, lineType.name())
                    .replace(TRANSPORT_MODE_IN_LINE_TEMPLATE, transportModeBlockInLine)
                    .replace(FLEXIBLE_LINE_TYPE_TEMPLATE, flexibleLineTypeBlock)
                    .replace(TRANSPORT_MODE_IN_SERVICE_JOURNEY_TEMPLATE, transportModeBlockInServiceJourney);
        }

        protected TestData withLineType(LineType lineType) {
            this.lineType = lineType;
            return this;
        }

        protected TestData withFlexibleLineType(FlexibleLineTypeEnumeration flexibleLineType) {
            this.flexibleLineTypeBlock = "<FlexibleLineType>" + flexibleLineType.value() + "</FlexibleLineType>";
            return this;
        }

        protected TestData withTransportModeAtLine(AllVehicleModesOfTransportEnumeration transportMode,
                                                   TransportSubMode transportSubMode) {
            transportModeBlockInLine = createTransportModeBlock(transportMode, transportSubMode);
            return this;
        }

        protected TestData withTransportModeAtServiceJourney(AllVehicleModesOfTransportEnumeration transportMode,
                                                             TransportSubMode transportSubMode) {
            transportModeBlockInServiceJourney = createTransportModeBlock(transportMode, transportSubMode);
            return this;
        }

        private String createTransportModeBlock(AllVehicleModesOfTransportEnumeration transportMode,
                                                TransportSubMode transportSubMode) {
            String value = transportMode.value();
            String subModeTagName = value.substring(0, 1).toUpperCase() + value.substring(1) + "Submode";

            return String.format("""
                            <TransportMode>%s</TransportMode>
                            <TransportSubmode>
                              <%s>%s</%s>
                            </TransportSubmode>
                              """,
                    transportMode.value(),
                    subModeTagName,
                    transportSubMode.name(),
                    subModeTagName
            );
        }
    }

    @Test
    void transportModeOnLineMatchesWithStopPlace() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        TestData testData = new TestData()
                .withTransportModeAtLine(AllVehicleModesOfTransportEnumeration.BUS, new TransportSubMode("localBus"));

        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.BUS, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void transportModeOverriddenOnServiceJourneyMatchesWithStopPlace() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        TestData testData = new TestData()
                .withTransportModeAtLine(AllVehicleModesOfTransportEnumeration.BUS, new TransportSubMode("localBus"))
                .withTransportModeAtServiceJourney(AllVehicleModesOfTransportEnumeration.METRO, new TransportSubMode("metro"));

        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.METRO, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void railReplacementBusServiceCanVisitRailReplacementBusStops() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        TestData testData = new TestData()
                .withTransportModeAtLine(AllVehicleModesOfTransportEnumeration.BUS, new TransportSubMode("railReplacementBus"));

        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(
                        VehicleModeEnumeration.BUS,
                        new TransportSubMode(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value())));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void railReplacementBusServiceCanOnlyVisitRailReplacementBusStops() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(AllVehicleModesOfTransportEnumeration.BUS, new TransportSubMode("railReplacementBus"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(
                        VehicleModeEnumeration.BUS,
                        new TransportSubMode(BusSubmodeEnumeration.EXPRESS_BUS.value())
                ));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void transportModeBusOnServiceJourneyShouldMatchWithTransportModeCoachOnStopPlace() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(
                        AllVehicleModesOfTransportEnumeration.BUS,
                        new TransportSubMode("localBus"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.COACH, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void transportModeCoachOnServiceJourneyShouldMatchWithTransportModeBusOnStopPlace() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(
                        AllVehicleModesOfTransportEnumeration.COACH,
                        new TransportSubMode("localBus"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.BUS, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void taxiCanStopOnBusStops() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(
                        AllVehicleModesOfTransportEnumeration.TAXI,
                        new TransportSubMode("communalTaxi"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.BUS, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void taxiCanStopOnCoachStops() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(
                        AllVehicleModesOfTransportEnumeration.TAXI,
                        new TransportSubMode("communalTaxi"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.COACH, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void taxiCannotStopOnStopOtherThanBusOrCoach() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(
                        AllVehicleModesOfTransportEnumeration.TAXI,
                        new TransportSubMode("communalTaxi"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.METRO, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(
                validationReport.getValidationReportEntries().stream().findFirst().map(ValidationReportEntry::getName),
                is(Optional.of("NETEX_TRANSPORT_MODE_1"))
        );
        assertThat(
                validationReport.getValidationReportEntries().stream().findFirst().map(ValidationReportEntry::getMessage),
                is(Optional.of("Invalid transport mode TAXI found in service journey with id " + TestData.SERVICE_JOURNEY_REF))
        );
    }

    @Test
    void validateOkWhenTransportModeNotFound() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(
                        AllVehicleModesOfTransportEnumeration.TAXI,
                        new TransportSubMode("communalTaxi"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(null);

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void transportModeMissMatchShouldGenerateValidationEntry() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        // Creating test data
        TestData testData = new TestData()
                .withTransportModeAtLine(
                        AllVehicleModesOfTransportEnumeration.METRO,
                        new TransportSubMode("communalTaxi"));

        // Mock findQuayId
        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        // Mock getTransportModeForQuayId
        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(
                        VehicleModeEnumeration.RAIL,
                        new TransportSubMode(RailSubmodeEnumeration.LONG_DISTANCE.value())));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(
                validationReport.getValidationReportEntries().stream().findFirst().map(ValidationReportEntry::getName),
                is(Optional.of("NETEX_TRANSPORT_MODE_1"))
        );
        assertThat(
                validationReport.getValidationReportEntries().stream().findFirst().map(ValidationReportEntry::getMessage),
                is(Optional.of("Invalid transport mode METRO found in service journey with id " + TestData.SERVICE_JOURNEY_REF))
        );
    }

    @Test
    void correctTransportModeOnFlexibleLineWithTypeFixedShouldBeValidated() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        TestData testData = new TestData()
                .withLineType(TestData.LineType.FlexibleLine)
                .withFlexibleLineType(FlexibleLineTypeEnumeration.FIXED)
                .withTransportModeAtLine(AllVehicleModesOfTransportEnumeration.BUS, new TransportSubMode("localBus"));

        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.BUS, null));

        ValidationReport validationReport = runValidation(testData, commonDataRepository, stopPlaceRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void flexibleLineWithTypeOtherThanFixedShouldNotBeAccepted() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        TestData testData = new TestData()
                .withLineType(TestData.LineType.FlexibleLine)
                .withFlexibleLineType(FlexibleLineTypeEnumeration.OTHER)
                .withTransportModeAtLine(AllVehicleModesOfTransportEnumeration.BUS, new TransportSubMode("localBus"));

        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.BUS, null));


        Exception exception = assertThrows(AntuException.class, () -> {
            runValidation(testData, commonDataRepository, stopPlaceRepository);
        });

        assertThat(exception.getMessage(), is("Unsupported FlexibleLineType in TransportModeValidator: other"));
    }

    @Test
    void flexibleLineWithoutFlexibleLineTypeShouldNotBeAccepted() {

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        TestData testData = new TestData()
                .withLineType(TestData.LineType.FlexibleLine)
                .withTransportModeAtLine(AllVehicleModesOfTransportEnumeration.BUS, new TransportSubMode("localBus"));

        Mockito.when(commonDataRepository.findQuayId(TestData.SCHEDULED_STOP_POINT_REF, TestData.VALIDATION_REPORT_ID))
                .thenReturn(TestData.QUAY_ID);

        Mockito.when(stopPlaceRepository.getTransportModesForQuayId(TestData.QUAY_ID))
                .thenReturn(new StopPlaceTransportModes(VehicleModeEnumeration.BUS, null));


        Exception exception = assertThrows(AntuException.class, () -> {
            runValidation(testData, commonDataRepository, stopPlaceRepository);
        });

        assertThat(exception.getMessage(), is("Missing FlexibleLineType for FlexibleLine"));
    }

    protected ValidationReport runValidation(TestData testData,
                                             CommonDataRepository commonDataRepository,
                                             StopPlaceRepository stopPlaceRepository) {

        TransportModeValidator validator = new TransportModeValidator(
                (code, message, dataLocation) ->
                        new ValidationReportEntry(message, code, ValidationReportEntrySeverity.ERROR),
                commonDataRepository,
                stopPlaceRepository
        );

        XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(testData.getData());

        ValidationContext validationContext =
                new ValidationContext(document, NETEX_XML_PARSER, TestData.CODE_SPACE, "lineFile.xml", Set.of(), List.of());

        ValidationReport validationReport = new ValidationReport(TestData.CODE_SPACE, TestData.VALIDATION_REPORT_ID);

        validator.validate(validationReport, validationContext);

        return validationReport;
    }
}