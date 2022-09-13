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

package no.entur.antu.routes.memorystore;

import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.entur.antu.Constants.TEMPORARY_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;


/**
 * Upload and download files to a memory store.
 * This is intended to be used for temporary files created during the validation process and shared between Kubernetes prods.
 * Using a memory store gives better performance than storing temporary files in Google Cloud Storage.
 */
@Component
public class MemoryStoreRoute extends BaseRouteBuilder {

    @Override
    public void configure() {

        from("direct:downloadBlobFromMemoryStore")
                .bean("temporaryFileRepository", "download(${header." + VALIDATION_REPORT_ID_HEADER + "},${header." + TEMPORARY_FILE_NAME + "})")
                .log(LoggingLevel.DEBUG, correlation() + "Returning from fetching file ${header." + TEMPORARY_FILE_NAME + "} from memory store.")
                .routeId("memory-store-download");

        from("direct:uploadBlobToMemoryStore")
                .bean("temporaryFileRepository", "upload(${header." + VALIDATION_REPORT_ID_HEADER + "},${header." + TEMPORARY_FILE_NAME + "}, ${body} )")
                .setBody(constant(""))
                .log(LoggingLevel.DEBUG, correlation() + "Stored file ${header." + TEMPORARY_FILE_NAME + "} in memory store.")
                .routeId("memory-store-upload");
    }
}
