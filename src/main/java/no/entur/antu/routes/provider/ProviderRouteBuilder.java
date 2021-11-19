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

package no.entur.antu.routes.provider;


import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refresh the provider register cache.
 */
@Component
public class ProviderRouteBuilder extends BaseRouteBuilder {

    private final String quartzTrigger;

    public ProviderRouteBuilder(@Value("${antu.provider.refresh.interval:trigger.repeatInterval=600000&trigger.repeatCount=-1&startDelayedSeconds=10&stateful=true}") String quartzTrigger) {
        super();
        this.quartzTrigger = quartzTrigger;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        from("quartz://antu/refreshProviderCache?" + quartzTrigger)
                .to("direct:refresh-provider-cache")
                .routeId("refresh-provider-cache-quartz");

        from("direct:refresh-provider-cache")
                .log(LoggingLevel.INFO, correlation() + "Refreshing provider cache")
                .bean("providerRepository", "refreshCache")
                .log(LoggingLevel.INFO, correlation() + "Refreshed provider cache")
                .routeId("refresh-provider-cache");


    }
}

