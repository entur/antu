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

import static no.entur.antu.Constants.DATASET_NB_COMMON_FILES;
import static no.entur.antu.Constants.FILENAME_DELIMITER;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.JOB_TYPE_VALIDATE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER;


/**
 * Extract NeTEx files from a NeTEx archive, save them individually in a bucket and
 * create a validation job for each common file.
 * Validation jobs for line files are created subsequently after all common files have been validated (see {@link CommonFilesBarrierRouteBuilder})
 *
 */
@Component
public class SplitDatasetRouteBuilder extends BaseRouteBuilder {

    private static final String PROP_ALL_NETEX_FILE_NAMES = "ALL_NETEX_FILE_NAMES";

    @Override
    public void configure() throws Exception {
        super.configure();


        from("direct:splitDataset")
                .streamCaching()
                .setHeader(FILE_HANDLE, header(VALIDATION_DATASET_FILE_HANDLE_HEADER))
                .to("direct:downloadNetexDataset")
                .log(LoggingLevel.INFO, correlation() + "Uploading NeTEx files")
                .to("direct:uploadSingleNetexFiles")
                .log(LoggingLevel.INFO, correlation() + "Uploaded NeTEx files")
                .process(exchange -> {
                    long nbCommonFiles = ((Set<String>) exchange.getProperty(PROP_ALL_NETEX_FILE_NAMES, Set.class)).stream().filter(fileName -> fileName.startsWith("_")).count();
                    exchange.getIn().setHeader(DATASET_NB_COMMON_FILES, nbCommonFiles);
                })
                .choice()
                .when(header(DATASET_NB_COMMON_FILES).isGreaterThan(0))
                .to("direct:createCommonFilesValidationJobs")
                .otherwise()
                // skip the common file barrier and go directly to the line file job creation step
                .process(exchange -> {
                    String allFileNames = String.join(FILENAME_DELIMITER, exchange.getProperty(PROP_ALL_NETEX_FILE_NAMES, Set.class));
                    exchange.getIn().setBody(allFileNames);
                })
                .to("direct:createLineFilesValidationJobs")
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
                .choice()
                .when(header(Exchange.FILE_NAME).endsWith(".xml"))
                .to("direct:uploadSingleNetexFile")
                .otherwise()
                .log(LoggingLevel.INFO, correlation() + "Ignoring non-XML file ${header." + Exchange.FILE_NAME + "}")
                .setBody(constant(""))
                .routeId("upload-single-netex-files");

        from("direct:createCommonFilesValidationJobs")
                .log(LoggingLevel.DEBUG, correlation() + "Creating common files validation jobs")
                .split(exchangeProperty(PROP_ALL_NETEX_FILE_NAMES))
                .filter(body().startsWith("_"))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_VALIDATE))
                .setHeader(Constants.DATASET_NB_NETEX_FILES, exchangeProperty(Exchange.SPLIT_SIZE))
                .setHeader(NETEX_FILE_NAME, body())
                .setHeader(FILE_HANDLE, simple(Constants.GCS_BUCKET_FILE_NAME))
                .process(exchange -> {
                    String allFileNames = String.join(FILENAME_DELIMITER, exchange.getProperty(PROP_ALL_NETEX_FILE_NAMES, Set.class));
                    exchange.getIn().setBody(allFileNames);
                })
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                //end split
                .end()
                .routeId("create-common-files-validation-jobs");

        from("direct:uploadSingleNetexFile")
                .setHeader(NETEX_FILE_NAME, header(Exchange.FILE_NAME))
                .marshal().zipFile()
                .setHeader(FILE_HANDLE, simple(Constants.GCS_BUCKET_FILE_NAME))
                .log(LoggingLevel.INFO, correlation() + "Uploading NeTEx file ${header." + NETEX_FILE_NAME + "} to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadBlobToMemoryStore")
                .routeId("upload-single-netex-file");

    }

    public static final class SingleNetexFileAggregationStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                Set<String> netexFileNames = new HashSet<>();
                newExchange.setProperty(PROP_ALL_NETEX_FILE_NAMES, netexFileNames);
                String currentFileName = newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
                if (currentFileName.endsWith(".xml.zip")) {
                    netexFileNames.add(removeZipExtension(currentFileName));
                }
                return newExchange;
            }
            String currentFileName = newExchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            if (currentFileName.endsWith(".xml.zip")) {
                Set<String> netexFileNames = oldExchange.getProperty(PROP_ALL_NETEX_FILE_NAMES, Set.class);
                netexFileNames.add(removeZipExtension(currentFileName));
            }

            return oldExchange;
        }

        private String removeZipExtension(String currentFileName) {
            return currentFileName.substring(0, currentFileName.length() - 4);
        }
    }
}
