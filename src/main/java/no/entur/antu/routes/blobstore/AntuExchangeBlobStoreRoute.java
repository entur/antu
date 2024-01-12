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
 */

package no.entur.antu.routes.blobstore;

import static no.entur.antu.Constants.FILE_HANDLE;

import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.services.AntuExchangeBlobStoreService;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class AntuExchangeBlobStoreRoute extends BaseRouteBuilder {

  private final AntuExchangeBlobStoreService antuExchangeBlobStoreService;

  public AntuExchangeBlobStoreRoute(
    AntuExchangeBlobStoreService antuExchangeBlobStoreService
  ) {
    this.antuExchangeBlobStoreService = antuExchangeBlobStoreService;
  }

  @Override
  public void configure() {
    from("direct:getAntuExchangeBlob")
      .bean(antuExchangeBlobStoreService, "getBlob")
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Returning from fetching file ${header." +
        FILE_HANDLE +
        "} from Antu Exchange bucket."
      )
      .routeId("blobstore-antu-exchange-download");
  }
}
