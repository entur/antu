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

package no.entur.antu.routes.organisation;

import static no.entur.antu.Constants.JOB_TYPE;
import static no.entur.antu.Constants.JOB_TYPE_REFRESH_ORGANISATION_CACHE;

import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refresh the organisation register cache.
 */
@Component
public class OrganisationRouteBuilder extends BaseRouteBuilder {

  private final String quartzTrigger;

  public OrganisationRouteBuilder(
    @Value(
      "${antu.organisation.refresh.interval:trigger.repeatInterval=600000&trigger.repeatCount=-1&stateful=true}"
    ) String quartzTrigger
  ) {
    super();
    this.quartzTrigger = quartzTrigger;
  }

  @Override
  public void configure() throws Exception {
    super.configure();
    from(
      "master:lockOnAntuRefreshOrganisationCache:quartz://antu/refreshOrganisationCache?" +
      quartzTrigger
    )
      .setHeader(JOB_TYPE, simple(JOB_TYPE_REFRESH_ORGANISATION_CACHE))
      .log(
        LoggingLevel.INFO,
        correlation() + "Scheduling organisation cache refresh job"
      )
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
      .routeId("refresh-organisation-cache-quartz");

    from(
      "master:lockOnAntuRefreshOrganisationCache:timer://antu/refreshOrganisationCacheAtStartup?repeatCount=1&delay=5000"
    )
      .routeId("prime-organisation-cache")
      .choice()
      .when(method("organisationRepository", "isEmpty"))
      .log(
        LoggingLevel.INFO,
        correlation() + "Organisation cache v3 is empty, priming cache"
      )
      .bean("organisationRepository", "refreshCache");

    from("direct:refreshOrganisationCache")
      .log(LoggingLevel.INFO, correlation() + "Refreshing organisation cache")
      .process(this::extendAckDeadline)
      .bean("organisationRepository", "refreshCache")
      .process(this::extendAckDeadline)
      .log(LoggingLevel.INFO, correlation() + "Refreshed organisation cache")
      .routeId("refresh-organisation-cache");
  }
}
