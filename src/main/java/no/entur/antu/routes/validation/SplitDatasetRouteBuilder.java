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
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.JOB_TYPE_VALIDATE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;


/**
 * Extract NeTEx files from a NeTEx archive and save them individually in a bucket. A Validation job is created for each NeTEx file.
 */
@Component
public class SplitDatasetRouteBuilder extends BaseRouteBuilder {

    public static final String ALL_NETEX_FILE_NAMES = "ALL_NETEX_FILE_NAMES";

    @Override
    public void configure() throws Exception {
        super.configure();


        from("direct:splitDataset")
                .streamCaching()
                .to("direct:downloadNetexDataset")
                .to("direct:uploadSingleNetexFiles")
                .to("direct:createValidationJobs")
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

        from("direct:uploadSingleNetexFiles")
                .split(new ZipSplitter()).aggregationStrategy(new SingleNetexFileAggregationStrategy())
                .streaming()
                .log(LoggingLevel.INFO, correlation() + "Processing NeTEx file ${header." + Exchange.FILE_NAME + "}")
                .filter(header(Exchange.FILE_NAME).not().endsWith(".xml"))
                .log(LoggingLevel.INFO, correlation() + "Ignoring non-XML file ${header." + Exchange.FILE_NAME + "}")
                // skip this file
                .stop()
                // end filter
                .end()
                .setHeader(NETEX_FILE_NAME, header(Exchange.FILE_NAME))
                .marshal().zipFile()
                .to("direct:uploadSingleNetexFile")
                .routeId("upload-single-netex-files");

        from("direct:createValidationJobs")
                .split(header(ALL_NETEX_FILE_NAMES))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_VALIDATE))
                .setHeader(Constants.DATASET_NB_NETEX_FILES, exchangeProperty(Exchange.SPLIT_SIZE))
                .setHeader(NETEX_FILE_NAME, body())
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                //end split
                .end()
                .routeId("create-validation-jobs");

        from("direct:uploadSingleNetexFile")
                .setHeader(FILE_HANDLE, simple(Constants.GCS_BUCKET_FILE_NAME))
                .log(LoggingLevel.INFO, correlation() + "Uploading NeTEx file ${header." + NETEX_FILE_NAME + "} to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadAntuBlob")
                .routeId("upload-single-netex-file");

    }

    public static final class SingleNetexFileAggregationStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                Set<String> netexFileNames = new HashSet<>();
                String currentFileName = newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                if (currentFileName.endsWith(".xml.zip")) {
                    netexFileNames.add(currentFileName);
                }
                newExchange.getIn().setHeader(ALL_NETEX_FILE_NAMES, netexFileNames);
                return newExchange;
            }
            String currentFileName = newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            if (currentFileName.endsWith(".xml.zip")) {
                Set<String> netexFileNames = oldExchange.getIn().getHeader(ALL_NETEX_FILE_NAMES, Set.class);
                netexFileNames.add(currentFileName);
            }

            return oldExchange;
        }
    }
}
