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

package no.entur.antu.routes.rest;

import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Ordered;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

/**
 * Readiness endpoint served by platform-http itself.
 * The actuator health group reports UP even when the platform-http routes are unreachable and
 * every /services/** request gets Spring's no-handler 404, so pointing the readiness probe here
 * is what keeps such a pod out of the Service endpoints. Answers from a constant with no
 * dependency on Redis, GCS or PubSub, so it never fails on a downstream outage.
 */
@Component
public class HealthRouteBuilder extends BaseRouteBuilder {

  private static final String PLAIN = "text/plain";

  /**
   * Each builder's rests are converted as that builder is processed, so this one must run after
   * the builder that declares restConfiguration().
   */
  @Override
  public int getOrder() {
    return Ordered.LOWEST;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    rest("/health")
      .apiDocs(false)
      .get()
      .bindingMode(RestBindingMode.off)
      .to("direct:health");

    from("direct:health")
      .setHeader(Exchange.CONTENT_TYPE, constant(PLAIN))
      .setBody(constant("OK"))
      .routeId("health");
  }
}
