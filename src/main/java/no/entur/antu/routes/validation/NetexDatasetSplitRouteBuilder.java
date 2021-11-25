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


import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.JOB_TYPE_VALIDATE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;


/**
 * Validate a NeTEx file upon notification from Marduk..
 */
@Component
public class NetexDatasetSplitRouteBuilder extends BaseRouteBuilder {

    private static final String GCS_BUCKET_FILE_NAME = "work/${header." + DATASET_CODESPACE + "}/${header." + VALIDATION_REPORT_ID + "}/${header." + NETEX_FILE_NAME + "}";

    @Override
    public void configure() throws Exception {
        super.configure();


        from("direct:splitDataset")
                .streamCaching()
                .to("direct:downloadNetexDataset")
                .split(new ZipSplitter()).streaming()
                .log(LoggingLevel.INFO, correlation() + "Processing NeTEx file ${header." + FILE_HANDLE + "}")
                .marshal().zipFile()
                .setHeader(NETEX_FILE_NAME, header(Exchange.FILE_NAME))
                .to("direct:uploadNetexFile")
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_VALIDATE))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                .routeId("split-dataset");

        from("direct:downloadNetexDataset")
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx dataset")
                .to("direct:getMardukBlob")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx file not found")
                .stop()
                //end filter
                .end()
                .routeId("download-netex-dataset");


        from("direct:uploadNetexFile")
                .setHeader(FILE_HANDLE, simple(GCS_BUCKET_FILE_NAME))
                .log(LoggingLevel.INFO, correlation() + "Uploading NeTEx file ${header." + NETEX_FILE_NAME + "} to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadAntuBlob")
                .routeId("upload-netex-file");


    }
}
