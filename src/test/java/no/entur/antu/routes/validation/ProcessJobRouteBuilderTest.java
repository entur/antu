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

import static no.entur.antu.Constants.*;

import java.util.Map;
import no.entur.antu.AntuRouteBuilderIntegrationTestBase;
import no.entur.antu.TestApp;
import no.entur.antu.config.cache.ValidationState;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class,
  properties = { "antu.netex.validation.entries.max=1" }
)
class ProcessJobRouteBuilderTest extends AntuRouteBuilderIntegrationTestBase {

  private static final String TEST_REPORT_ID = "reportId";
  private static final String OTHER_TEST_REPORT_ID = "other_reportId";

  @Autowired
  ValidationStateRepository validationStateRepository;

  @Produce("direct:processJob")
  protected ProducerTemplate processJob;

  @EndpointInject("mock:notifyStatus")
  protected MockEndpoint notifyStatus;

  @EndpointInject("mock:validateNetex")
  protected MockEndpoint validateNetex;

  @EndpointInject("mock:refreshStopCache")
  protected MockEndpoint refreshStopCache;

  @Test
  void testIgnoreValidationJobAfterCompletion() throws Exception {
    AdviceWith.adviceWith(
      context,
      "process-job",
      a ->
        a
          .interceptSendToEndpoint("direct:validateNetex")
          .skipSendToOriginalEndpoint()
          .to("mock:validateNetex")
    );

    validateNetex.expectedMessageCount(0);
    context.start();
    Map<String, Object> headers = Map.of(
      VALIDATION_REPORT_ID_HEADER,
      OTHER_TEST_REPORT_ID,
      JOB_TYPE,
      JOB_TYPE_VALIDATE
    );
    processJob.sendBodyAndHeaders(" ", headers);
    validateNetex.assertIsSatisfied();
  }

  @Test
  void testAcceptValidationJobBeforeCompletion() throws Exception {
    AdviceWith.adviceWith(
      context,
      "process-job",
      a ->
        a
          .interceptSendToEndpoint("direct:validateNetex")
          .skipSendToOriginalEndpoint()
          .to("mock:validateNetex")
    );

    validateNetex.expectedMessageCount(1);

    context.start();

    validationStateRepository.createValidationStateIfMissing(
      TEST_REPORT_ID,
      new ValidationState()
    );

    Map<String, Object> headers = Map.of(
      VALIDATION_REPORT_ID_HEADER,
      TEST_REPORT_ID,
      JOB_TYPE,
      JOB_TYPE_VALIDATE
    );
    processJob.sendBodyAndHeaders(" ", headers);
    validateNetex.assertIsSatisfied();
  }

  @Test
  void testAcceptOtherTypesOfJob() throws Exception {
    AdviceWith.adviceWith(
      context,
      "process-job",
      a ->
        a
          .interceptSendToEndpoint("direct:refreshStopCache")
          .skipSendToOriginalEndpoint()
          .to("mock:refreshStopCache")
    );

    refreshStopCache.expectedMessageCount(1);

    context.start();
    Map<String, Object> headers = Map.of(JOB_TYPE, JOB_TYPE_REFRESH_STOP_CACHE);
    processJob.sendBodyAndHeaders(" ", headers);
    refreshStopCache.assertIsSatisfied();
  }
}
