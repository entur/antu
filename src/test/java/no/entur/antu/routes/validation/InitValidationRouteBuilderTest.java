/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.antu.routes.validation;

import no.entur.antu.AntuRouteBuilderIntegrationTestBase;
import no.entur.antu.Constants;
import no.entur.antu.TestApp;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.util.TestValidationReportUtil;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_REPORTS;
import static no.entur.antu.Constants.BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.STATUS_VALIDATION_FAILED;
import static no.entur.antu.Constants.STATUS_VALIDATION_STARTED;
import static no.entur.antu.Constants.VALIDATION_CLIENT_HEADER;
import static no.entur.antu.Constants.VALIDATION_CLIENT_KAKKA;
import static no.entur.antu.Constants.VALIDATION_CLIENT_MARDUK;
import static no.entur.antu.Constants.VALIDATION_CORRELATION_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_PROFILE_STOP;
import static no.entur.antu.Constants.VALIDATION_PROFILE_TIMETABLE;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;
import static no.entur.antu.Constants.VALIDATION_STAGE_HEADER;
import static no.entur.antu.util.TestValidationReportUtil.getValidationReport;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApp.class, properties = {
        "antu.netex.validation.entries.max=1"})
class InitValidationRouteBuilderTest extends AntuRouteBuilderIntegrationTestBase {

    private static final String TEST_DATASET_CODESPACE_FLB = "flb";
    private static final String TEST_DATASET_CODESPACE_AVI = "avi";
    public static final String TEST_DATASET_STOP_PLACE_CODESPACE = "nsr";

    private static final String TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME = "rb_flb-aggregated-netex.zip";
    private static final String TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME = "rb_flb-aggregated-netex-schema-error.zip";

    private static final String TEST_DATASET_STOP_PLACE_FILE_NAME = "stopdata.zip";

    private static final String VALIDATION_STAGE_PREVALIDATION = "EnturValidationStagePreValidation";

    private static final String TEST_DATASET_NO_DUPLICATED_ID = "rb_avi-aggregated-netex.zip";
    private static final String TEST_DATASET_DUPLICATED_ID = "rb_avi-aggregated-netex-duplicated-id.zip";


    @Produce("direct:initDatasetValidation")
    protected ProducerTemplate initDatasetValidation;

    @EndpointInject("mock:notifyStatus")
    protected MockEndpoint notifyStatus;

    @TestConfiguration
    static class TestContextConfiguration {
        @Bean
        @Primary
        public OrganisationRepository organisationRepository() {
            return new OrganisationRepository() {
                @Override
                public void refreshCache() {

                }

                @Override
                public Set<String> getWhitelistedAuthorityIds(String codespace) {
                    if ("avi".equals(codespace)) {
                        return Set.of("AVI:Authority:Avinor");
                    }
                    if ("flb".equals(codespace)) {
                        return Set.of("FLB:Authority:XXX", "FLB:Authority:YYY");
                    }
                    return Collections.emptySet();

                }
            };
        }

        @Bean
        @Primary
        public StopPlaceRepository stopPlaceRepository() {
            return new StopPlaceRepository() {
                @Override
                public Set<String> getStopPlaceIds() {
                    return Collections.emptySet();
                }

                @Override
                public Set<String> getQuayIds() {
                    return Collections.emptySet();
                }

                @Override
                public void refreshCache() {
                }
            };
        }
    }

    @Test
    void testValidateAuthority() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));

        notifyStatus.expectedMessageCount(2);
        notifyStatus.setResultWaitTime(15000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE_FLB + '/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER, datasetBlobName);
        headers.put(Constants.DATASET_REFERENTIAL, "flb");
        headers.put(Constants.VALIDATION_STAGE_HEADER, VALIDATION_STAGE_PREVALIDATION);
        headers.put(Constants.VALIDATION_CLIENT_HEADER, VALIDATION_CLIENT_MARDUK);
        headers.put(Constants.VALIDATION_PROFILE_HEADER, VALIDATION_PROFILE_TIMETABLE);
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyStatus.assertIsSatisfied();
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_FAILED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(DATASET_REFERENTIAL) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> VALIDATION_STAGE_PREVALIDATION.equals(exchange.getIn().getHeader(VALIDATION_STAGE_HEADER))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> VALIDATION_CLIENT_MARDUK.equals(exchange.getIn().getHeader(VALIDATION_CLIENT_HEADER))));
    }

    @Test
    void testValidateStopPlaceData() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));

        notifyStatus.expectedMessageCount(2);
        notifyStatus.setResultWaitTime(15000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_STOP_PLACE_FILE_NAME);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_STOP_PLACE_FILE_NAME);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE_FLB + '/' + TEST_DATASET_STOP_PLACE_FILE_NAME;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER, datasetBlobName);
        headers.put(Constants.DATASET_REFERENTIAL, TEST_DATASET_STOP_PLACE_CODESPACE);
        headers.put(Constants.VALIDATION_STAGE_HEADER, VALIDATION_STAGE_PREVALIDATION);
        headers.put(Constants.VALIDATION_CLIENT_HEADER, VALIDATION_CLIENT_KAKKA);
        headers.put(Constants.VALIDATION_PROFILE_HEADER, VALIDATION_PROFILE_STOP);
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyStatus.assertIsSatisfied();
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_FAILED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(DATASET_REFERENTIAL) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> VALIDATION_STAGE_PREVALIDATION.equals(exchange.getIn().getHeader(VALIDATION_STAGE_HEADER))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> VALIDATION_CLIENT_KAKKA.equals(exchange.getIn().getHeader(VALIDATION_CLIENT_HEADER))));

        String validationReportId = notifyStatus.getExchanges().stream().findFirst().orElseThrow().getIn().getHeader(VALIDATION_REPORT_ID_HEADER, String.class);

        String reportBlobName = BLOBSTORE_PATH_ANTU_REPORTS + TEST_DATASET_STOP_PLACE_CODESPACE + VALIDATION_REPORT_PREFIX + validationReportId + VALIDATION_REPORT_SUFFIX;
        InputStream reportInputStream = antuInMemoryBlobStoreRepository.getBlob(reportBlobName);
        ValidationReport validationReport = getValidationReport(reportInputStream);
        Assertions.assertTrue(validationReport.hasError());

    }


    @Test
    void testValidateSchemaMoreThanMaxError() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));

        notifyStatus.expectedMessageCount(2);
        notifyStatus.setResultWaitTime(15000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE_FLB + '/' + TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER, datasetBlobName);
        headers.put(Constants.DATASET_REFERENTIAL, TEST_DATASET_CODESPACE_FLB);
        headers.put(Constants.VALIDATION_STAGE_HEADER, VALIDATION_STAGE_PREVALIDATION);
        headers.put(Constants.VALIDATION_CLIENT_HEADER, VALIDATION_CLIENT_MARDUK);
        headers.put(Constants.VALIDATION_PROFILE_HEADER, VALIDATION_PROFILE_TIMETABLE);
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyStatus.assertIsSatisfied();

        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(DATASET_REFERENTIAL) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_FAILED.equals(exchange.getIn().getBody(String.class))));

    }

    @Test
    void testValidateNoDuplicatedId() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));

        notifyStatus.expectedMessageCount(2);
        notifyStatus.setResultWaitTime(300000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_NO_DUPLICATED_ID);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_NO_DUPLICATED_ID);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE_AVI + '/' + TEST_DATASET_NO_DUPLICATED_ID;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER, datasetBlobName);
        headers.put(Constants.DATASET_REFERENTIAL, TEST_DATASET_CODESPACE_AVI);
        headers.put(Constants.VALIDATION_STAGE_HEADER, VALIDATION_STAGE_PREVALIDATION);
        headers.put(Constants.VALIDATION_CLIENT_HEADER, VALIDATION_CLIENT_MARDUK);
        headers.put(Constants.VALIDATION_PROFILE_HEADER, VALIDATION_PROFILE_TIMETABLE);
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyStatus.assertIsSatisfied();

        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(DATASET_REFERENTIAL) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));

        String reportId = notifyStatus.getExchanges().stream().findFirst().orElseThrow().getIn().getHeader(VALIDATION_REPORT_ID_HEADER, String.class);
        InputStream validationReportAsStream = antuInMemoryBlobStoreRepository.getBlob(BLOBSTORE_PATH_ANTU_REPORTS + TEST_DATASET_CODESPACE_AVI + VALIDATION_REPORT_PREFIX + reportId + VALIDATION_REPORT_SUFFIX);
        Assertions.assertNotNull(validationReportAsStream, "Validation report not found");
        ValidationReport validationReport = TestValidationReportUtil.getValidationReport(validationReportAsStream);
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream().noneMatch(validationReportEntry -> "NeTEx ID duplicated across files".equals(validationReportEntry.getName())));
    }


    @Test
    void testValidateDuplicatedId() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));

        notifyStatus.expectedMessageCount(2);
        notifyStatus.setResultWaitTime(300000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_DUPLICATED_ID);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_DUPLICATED_ID);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE_AVI + '/' + TEST_DATASET_DUPLICATED_ID;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER, datasetBlobName);
        headers.put(Constants.DATASET_REFERENTIAL, TEST_DATASET_CODESPACE_AVI);
        headers.put(Constants.VALIDATION_STAGE_HEADER, VALIDATION_STAGE_PREVALIDATION);
        headers.put(Constants.VALIDATION_CLIENT_HEADER, VALIDATION_CLIENT_MARDUK);
        headers.put(Constants.VALIDATION_PROFILE_HEADER, VALIDATION_PROFILE_TIMETABLE);
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyStatus.assertIsSatisfied();

        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(DATASET_REFERENTIAL) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));

        String reportId = notifyStatus.getExchanges().stream().findFirst().orElseThrow().getIn().getHeader(VALIDATION_REPORT_ID_HEADER, String.class);
        InputStream validationReportAsStream = antuInMemoryBlobStoreRepository.getBlob(BLOBSTORE_PATH_ANTU_REPORTS + TEST_DATASET_CODESPACE_AVI + VALIDATION_REPORT_PREFIX + reportId + VALIDATION_REPORT_SUFFIX);
        Assertions.assertNotNull(validationReportAsStream, "Validation report not found");
        ValidationReport validationReport = TestValidationReportUtil.getValidationReport(validationReportAsStream);
        Assertions.assertTrue(validationReport.getValidationReportEntries().stream().anyMatch(validationReportEntry -> "NeTEx ID duplicated across files".equals(validationReportEntry.getName())));

    }


}
