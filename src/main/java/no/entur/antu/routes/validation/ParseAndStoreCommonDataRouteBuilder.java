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


import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.util.StopWatch;
import org.springframework.stereotype.Component;

import static no.entur.antu.Constants.*;
import static no.entur.antu.routes.memorystore.MemoryStoreRoute.MEMORY_STORE_FILE_NAME;

@Component
public class ParseAndStoreCommonDataRouteBuilder extends BaseRouteBuilder {

    private static final String PROP_STOP_WATCH = "PROP_STOP_WATCH";

    private final CommonDataRepository commonDataRepository;

    public ParseAndStoreCommonDataRouteBuilder(CommonDataRepository commonDataRepository) {
        this.commonDataRepository = commonDataRepository;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:storeCommonData")
                .log(LoggingLevel.INFO, correlation() + "Parsing NeTEx file ${header." + FILE_HANDLE + "}")
                .setProperty(PROP_STOP_WATCH, StopWatch::new)
                .doTry()
                    .setHeader(MEMORY_STORE_FILE_NAME, header(NETEX_COMMON_FILE_NAME))
                    .to("direct:downloadSingleNetexFileFromMemoryStore")
                    .process(exchange -> commonDataRepository.loadCommonDataCache(exchange.getIn().getBody(byte[].class)))
                .doCatch(Exception.class)
                    .log(LoggingLevel.ERROR, correlation() + "System error while parsing the NeTEx file ${header." + FILE_HANDLE + "}: ${exception.message} stacktrace: ${exception.stacktrace}")
                .stop()
                // end catch
                .end()
                .log(LoggingLevel.INFO, correlation() + "Parsed NeTEx file ${header." + NETEX_FILE_NAME + "} in ${exchangeProperty." + PROP_STOP_WATCH + ".taken()} ms")
                .routeId("store-common-data");
    }
}