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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.entur.antu.AntuRouteBuilderIntegrationTestBase;
import no.entur.antu.Constants;
import no.entur.antu.TestApp;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_REPORTS;
import static no.entur.antu.Constants.BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED;
import static no.entur.antu.Constants.VALIDATION_CORRELATION_ID_HEADER;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.STATUS_VALIDATION_OK;
import static no.entur.antu.Constants.STATUS_VALIDATION_STARTED;
import static no.entur.antu.Constants.VALIDATION_CLIENT_HEADER;
import static no.entur.antu.Constants.VALIDATION_CLIENT_MARDUK;
import static no.entur.antu.Constants.VALIDATION_PROFILE_TIMETABLE_SWEDEN;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;
import static no.entur.antu.Constants.VALIDATION_STAGE_HEADER;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApp.class, properties = {
        "antu.netex.validation.entries.max=1",
        "antu.netex.job.consumers=2"})
class SwedenDatasetValidationTest extends AntuRouteBuilderIntegrationTestBase {

    private static final String TEST_DATASET_CODESPACE = "SAM";
    private static final String TEST_DATASET_SWEDEN_VALIDATION_FILE_NAME = "varmland.zip";

    private static final String VALIDATION_STAGE_PREVALIDATION = "EnturValidationStagePreValidation";


    @Produce("direct:initDatasetValidation")
    protected ProducerTemplate initDatasetValidation;

    @EndpointInject("mock:notifyStatus")
    protected MockEndpoint notifyStatus;

    @Test
    void testValidateDataset() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyStatus").skipSendToOriginalEndpoint()
                .to("mock:notifyStatus"));

        notifyStatus.expectedMessageCount(2);
        notifyStatus.setResultWaitTime(150000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_SWEDEN_VALIDATION_FILE_NAME);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_SWEDEN_VALIDATION_FILE_NAME);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE + '/' + TEST_DATASET_SWEDEN_VALIDATION_FILE_NAME;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER, datasetBlobName);
        headers.put(Constants.DATASET_REFERENTIAL, TEST_DATASET_CODESPACE);
        headers.put(Constants.VALIDATION_STAGE_HEADER, VALIDATION_STAGE_PREVALIDATION);
        headers.put(Constants.VALIDATION_CLIENT_HEADER, VALIDATION_CLIENT_MARDUK);
        headers.put(Constants.VALIDATION_PROFILE_HEADER, VALIDATION_PROFILE_TIMETABLE_SWEDEN);
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyStatus.assertIsSatisfied();
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_OK.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(DATASET_REFERENTIAL) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_CORRELATION_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> exchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER) != null));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> VALIDATION_STAGE_PREVALIDATION.equals(exchange.getIn().getHeader(VALIDATION_STAGE_HEADER))));
        Assertions.assertTrue(notifyStatus.getExchanges().stream().allMatch(exchange -> VALIDATION_CLIENT_MARDUK.equals(exchange.getIn().getHeader(VALIDATION_CLIENT_HEADER))));

        String validationReportId = notifyStatus.getExchanges().stream().findFirst().orElseThrow().getIn().getHeader(VALIDATION_REPORT_ID_HEADER, String.class);

        String reportBlobName = BLOBSTORE_PATH_ANTU_REPORTS + TEST_DATASET_CODESPACE + VALIDATION_REPORT_PREFIX + validationReportId + VALIDATION_REPORT_SUFFIX;
        InputStream reportInputStream = antuInMemoryBlobStoreRepository.getBlob(reportBlobName);
        assert reportInputStream != null;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        ValidationReport validationReport = objectMapper.readerFor(ValidationReport.class).readValue(reportInputStream);
        Assertions.assertFalse(validationReport.hasError());
    }


}
