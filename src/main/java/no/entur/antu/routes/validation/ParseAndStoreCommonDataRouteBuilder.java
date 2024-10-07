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

package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.PROP_NETEX_FILE_CONTENT;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;

import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.util.StopWatch;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.springframework.stereotype.Component;

@Component
public class ParseAndStoreCommonDataRouteBuilder extends BaseRouteBuilder {

  private static final String PROP_STOP_WATCH = "PROP_STOP_WATCH";

  private final NetexDataRepository commonDataRepository;

  public ParseAndStoreCommonDataRouteBuilder(
    NetexDataRepository commonDataRepository
  ) {
    this.commonDataRepository = commonDataRepository;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:storeCommonData")
      .filter(header(NETEX_FILE_NAME).startsWith("_"))
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Extracting common data from NeTEx file ${header." +
        NETEX_FILE_NAME +
        "}"
      )
      .setProperty(PROP_STOP_WATCH, StopWatch::new)
      .doTry()
      .process(exchange ->
        commonDataRepository.loadCommonDataCache(
          exchange.getProperty(PROP_NETEX_FILE_CONTENT, byte[].class),
          exchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER, String.class)
        )
      )
      .doCatch(Exception.class)
      .log(
        LoggingLevel.ERROR,
        correlation() +
        "System error while parsing the NeTEx file ${header." +
        FILE_HANDLE +
        "}: " +
        "${exception.message} stacktrace: ${exception.stacktrace}"
      )
      .stop()
      // end catch
      .end()
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Extracted common data from NeTEx file ${header." +
        NETEX_FILE_NAME +
        "} in " +
        "${exchangeProperty." +
        PROP_STOP_WATCH +
        ".taken()} ms"
      )
      .routeId("store-common-data");
  }
}
