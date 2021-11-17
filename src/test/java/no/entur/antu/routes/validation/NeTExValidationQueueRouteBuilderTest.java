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
import no.entur.antu.organisation.OrganisationRegistry;
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

import static no.entur.antu.Constants.BLOBSTORE_PATH_INBOUND_RECEIVED;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApp.class)
class NeTExValidationQueueRouteBuilderTest extends AntuRouteBuilderIntegrationTestBase {

    private static final String TEST_DATASET_CODESPACE = "flb";
    private static final String TEST_DATASET_FILE_NAME = "rb_flb-aggregated-netex.zip";

    @Produce("direct:netexValidationQueue")
    protected ProducerTemplate antuNetexValidationQueueProducerTemplate;

    @EndpointInject("mock:notifyMarduk")
    protected MockEndpoint notifyMarduk;

    @TestConfiguration
    static class EmployeeServiceImplTestContextConfiguration {
        @Bean
        @Primary
        public OrganisationRegistry organisationRegistry() {
            return new OrganisationRegistry() {
                @Override
                public void refreshCache() {

                }

                @Override
                public Set<String> getWhitelistedAuthorityIds(String codespace) {
                    return Set.of("FLB:Authority:FLB");
                }
            };
        }
    }


    @Test
    void testValidateNetex() throws Exception {

        AdviceWith.adviceWith(context, "netex-validation-queue", a -> a.interceptSendToEndpoint("direct:notifyMarduk").skipSendToOriginalEndpoint()
                .to("mock:notifyMarduk"));

        notifyMarduk.expectedMessageCount(2);
        notifyMarduk.setResultWaitTime(15000);

        InputStream testDatasetAsStream = getClass().getResourceAsStream("/rb_flb-aggregated-netex.zip");
        Assertions.assertNotNull(testDatasetAsStream);
        String datasetBlobName = BLOBSTORE_PATH_INBOUND_RECEIVED + TEST_DATASET_CODESPACE + '/' + TEST_DATASET_FILE_NAME;
        mardukInMemoryBlobStoreRepository.uploadBlob(datasetBlobName,
                testDatasetAsStream, true);


        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.FILE_HANDLE, datasetBlobName);
        antuNetexValidationQueueProducerTemplate.sendBodyAndHeaders(" ", headers);
        notifyMarduk.assertIsSatisfied();
        Assertions.assertTrue(notifyMarduk.getExchanges().stream().anyMatch(exchange ->  NeTExValidationQueueRouteBuilder.STATUS_VALIDATION_STARTED.equals(exchange.getIn().getBody(String.class))));
        Assertions.assertTrue(notifyMarduk.getExchanges().stream().anyMatch(exchange ->  NeTExValidationQueueRouteBuilder.STATUS_VALIDATION_OK.equals(exchange.getIn().getBody(String.class))));


    }


}
