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

package no.entur.antu.routes.stop;


import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Refresh the stop place register cache.
 */
@Component
public class StopRouteBuilder extends BaseRouteBuilder {

    private final String quartzTrigger;

    public StopRouteBuilder(@Value("${antu.stop.refresh.interval:trigger.repeatCount=0}") String quartzTrigger) {
        super();
        this.quartzTrigger = quartzTrigger;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

/*        from("master:lockOnAntuRefreshStopCacheAtStartup:quartz://antu/refreshStopPlaceCacheAtStartup?" + "?trigger.repeatCount=0")
                .log(LoggingLevel.INFO, correlation() + "Refreshing stop place cache at startup")
                .to("direct:refresh-stop-cache")
                .routeId("refresh-stop-cache-at-startup");
*/
        from("master:lockOnAntuRefreshStopCachePeriodically:quartz://antu/refreshStopPlaceCachePeriodically?" + quartzTrigger)
                .log(LoggingLevel.INFO, correlation() + "Refreshing stop place cache periodically")
                .to("direct:refresh-stop-cache")
                .routeId("refresh-stop-cache-periodically");

        from("direct:refresh-stop-cache")
                .log(LoggingLevel.INFO, correlation() + "Refreshing stop place cache")
                .bean("stopPlaceRepository", "refreshCache")
                .log(LoggingLevel.INFO, correlation() + "Refreshed stop place cache")
                .routeId("refresh-stop-cache");
    }
}

