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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestApp.class, properties = {
        "antu.netex.validation.entries.max=1"})
class ValidateFileRouteBuilderTest extends AntuRouteBuilderIntegrationTestBase {

    @Produce("direct:saveValidationReport")
    protected ProducerTemplate saveValidationReport;

    @EndpointInject("mock:uploadValidationReport")
    protected MockEndpoint uploadValidationReport;

    @Test
    void testJSonSerialization() throws Exception {

        ValidationReport validationReport = new ValidationReport("codespace", "reportId");
        LocalDateTime creationDateAsObject = validationReport.getCreationDate();

        AdviceWith.adviceWith(context, "save-validation-report", a -> {

                    a.weaveAddFirst().process(exchange -> exchange.setProperty(ValidateFilesRouteBuilder.PROP_VALIDATION_REPORT, validationReport ));

            a.interceptSendToEndpoint("direct:uploadValidationReport").skipSendToOriginalEndpoint()
                    .to("mock:uploadValidationReport");
        }
        );

        uploadValidationReport.expectedMessageCount(1);
        uploadValidationReport.setResultWaitTime(15000);

        context.start();
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.DATASET_CODESPACE, "FLB");
        saveValidationReport.sendBodyAndHeaders(" ", headers);
        uploadValidationReport.assertIsSatisfied();
        String body = uploadValidationReport.getExchanges().stream().findFirst().orElseThrow().getIn().getBody(String.class);
        Assertions.assertNotNull(body);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode creationDate = objectMapper.readTree(body).get("creationDate");
        Assertions.assertNotNull(creationDate);
        String creationDateAsString = creationDate.asText();
        Assertions.assertNotNull(creationDateAsString);
        Assertions.assertEquals(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(creationDateAsObject), creationDateAsString);

    }

}
