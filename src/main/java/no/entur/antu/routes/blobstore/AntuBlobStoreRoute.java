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

package no.entur.antu.routes.blobstore;

import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.services.AntuBlobStoreService;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.entur.antu.Constants.FILE_HANDLE;


@Component
public class AntuBlobStoreRoute extends BaseRouteBuilder {

    private final AntuBlobStoreService antuBlobStoreService;

    public AntuBlobStoreRoute(AntuBlobStoreService antuBlobStoreService) {
        this.antuBlobStoreService = antuBlobStoreService;
    }

    @Override
    public void configure() {

        from("direct:getAntuBlob")
                .to(logDebugShowAll())
                .bean(antuBlobStoreService, "getBlob")
                .to(logDebugShowAll())
                .log(LoggingLevel.INFO, correlation() + "Returning from fetching file ${header." + FILE_HANDLE + "} from Antu bucket.")
                .routeId("blobstore-antu-download");

        from("direct:uploadAntuBlob")
                .to(logDebugShowAll())
                .bean(antuBlobStoreService, "uploadBlob")
                .setBody(simple(""))
                .to(logDebugShowAll())
                .log(LoggingLevel.INFO, correlation() + "Stored file ${header." + FILE_HANDLE + "} in Antu bucket.")
                .routeId("blobstore-antu-upload");
    }
}
