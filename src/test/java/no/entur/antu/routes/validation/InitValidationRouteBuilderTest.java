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
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static no.entur.antu.Constants.BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED;
import static no.entur.antu.Constants.STATUS_VALIDATION_FAILED;
import static no.entur.antu.Constants.STATUS_VALIDATION_OK;
import static no.entur.antu.Constants.STATUS_VALIDATION_STARTED;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApp.class, properties = {
        "antu.netex.validation.entries.max=1"})
class InitValidationRouteBuilderTest extends AntuRouteBuilderIntegrationTestBase {

    private static final String TEST_DATASET_CODESPACE = "flb";
    private static final String TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME = "rb_flb-aggregated-netex.zip";
    private static final String TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME = "rb_flb-aggregated-netex-schema-error.zip";

    @Produce("direct:initDatasetValidation")
    protected ProducerTemplate initDatasetValidation;

    @EndpointInject("mock:notifyMarduk")
    protected MockEndpoint notifyMarduk;

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
                    return Set.of("FLB:Authority:XXX", "FLB:Authority:YYY");
                }
            };
        }

    }

    @Test
    void testValidateAuthority() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyMarduk").skipSendToOriginalEndpoint()
                .to("mock:notifyMarduk"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyMarduk").skipSendToOriginalEndpoint()
                .to("mock:notifyMarduk"));

        notifyMarduk.expectedMessageCount(2);
        notifyMarduk.setResultWaitTime(15000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE + '/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.FILE_HANDLE, datasetBlobName);
        headers.put(Constants.DATASET_CODESPACE, "FLB");
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyMarduk.assertIsSatisfied();
        Assertions.assertTrue(notifyMarduk.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyMarduk.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_OK.equals(exchange.getIn().getBody(String.class))));


    }

    @Test
    void testValidateSchemaMoreThanMaxError() throws Exception {

        AdviceWith.adviceWith(context, "init-dataset-validation", a -> a.interceptSendToEndpoint("direct:notifyMarduk").skipSendToOriginalEndpoint()
                .to("mock:notifyMarduk"));
        AdviceWith.adviceWith(context, "aggregate-reports", a -> a.interceptSendToEndpoint("direct:notifyMarduk").skipSendToOriginalEndpoint()
                .to("mock:notifyMarduk"));

        notifyMarduk.expectedMessageCount(2);
        notifyMarduk.setResultWaitTime(15000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME);
        Assertions.assertNotNull(testDatasetAsStream, "Test dataset file not found: " + TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME);
        String datasetBlobName = BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED + TEST_DATASET_CODESPACE + '/' + TEST_DATASET_SCHEMA_VALIDATION_FILE_NAME;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName, testDatasetAsStream);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.FILE_HANDLE, datasetBlobName);
        headers.put(Constants.DATASET_CODESPACE, "FLB");
        initDatasetValidation.sendBodyAndHeaders(" ", headers);
        notifyMarduk.assertIsSatisfied();
        Assertions.assertTrue(notifyMarduk.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyMarduk.getExchanges().stream().anyMatch(exchange -> STATUS_VALIDATION_FAILED.equals(exchange.getIn().getBody(String.class))));

    }


}
