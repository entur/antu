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
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.entur.netex.validation.validator.ValidationReport;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.entur.antu.Constants.AGGREGATED_VALIDATION_REPORT;
import static no.entur.antu.Constants.CORRELATION_ID;
import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.DATASET_NB_NETEX_FILES;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.DATASET_STATUS;
import static no.entur.antu.Constants.FILENAME_DELIMITER;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.JOB_TYPE_AGGREGATE_REPORTS;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.STATUS_VALIDATION_FAILED;
import static no.entur.antu.Constants.STATUS_VALIDATION_OK;
import static no.entur.antu.Constants.VALIDATION_CLIENT_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;
import static no.entur.antu.Constants.VALIDATION_STAGE_HEADER;


/**
 * Aggregate validation reports.
 */
@Component
public class AggregateValidationReportsRouteBuilder extends BaseRouteBuilder {

    private static final String PROP_DATASET_NETEX_FILE_NAMES = "EnturDatasetNetexFileNames";

    @Override
    public void configure() throws Exception {
        super.configure();

        from("master:lockOnAntuReportAggregationQueue:google-pubsub:{{antu.pubsub.project.id}}:AntuReportAggregationQueue")
                .process(this::removeSynchronizationForAggregatedExchange)
                .aggregate(header(VALIDATION_REPORT_ID)).aggregationStrategy(new ValidationReportAggregationStrategy()).completionTimeout(1800000)
                .process(this::addSynchronizationForAggregatedExchange)
                .log(LoggingLevel.INFO, correlation() + "Aggregated ${exchangeProperty.CamelAggregatedSize} validation reports (aggregation completion triggered by ${exchangeProperty.CamelAggregatedCompletedBy}).")

                .setBody(exchangeProperty(PROP_DATASET_NETEX_FILE_NAMES))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_AGGREGATE_REPORTS))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                .routeId("aggregate-reports-pubsub");

        from("direct:aggregateReports")
                .log(LoggingLevel.INFO, correlation() + "Merging individual reports")

                .process(exchange -> {
                    String codespace = exchange.getIn().getHeader(DATASET_CODESPACE, String.class);
                    String validationReportId = exchange.getIn().getHeader(VALIDATION_REPORT_ID, String.class);
                    ValidationReport validationReport = new ValidationReport(codespace, validationReportId);
                    exchange.getIn().setHeader(AGGREGATED_VALIDATION_REPORT, validationReport);
                })
                .convertBodyTo(String.class)
                .split(method(ReverseSortedFileNameSplitter.class, "split")).delimiter(FILENAME_DELIMITER)
                .log(LoggingLevel.INFO, correlation() + "Merging file ${body}.json")
                .setHeader(NETEX_FILE_NAME, body())
                .to("direct:downloadValidationReport")
                .unmarshal().json(JsonLibrary.Jackson, ValidationReport.class)
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getIn().getBody(ValidationReport.class);
                    ValidationReport aggregatedValidationReport = exchange.getIn().getHeader(AGGREGATED_VALIDATION_REPORT, ValidationReport.class);
                    aggregatedValidationReport.addAllValidationReportEntries(validationReport.getValidationReportEntries());

                })
                // end splitter
                .end()
                .log(LoggingLevel.INFO, correlation() + "Completed reports merging")
                .setBody(header(AGGREGATED_VALIDATION_REPORT))
                .choice()
                .when(simple("${body.hasError()}"))
                .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_FAILED))
                .log(LoggingLevel.INFO, correlation() + "Validation errors found")
                .otherwise()
                .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_OK))
                .log(LoggingLevel.INFO, correlation() + "No validation error")
                .end()
                .marshal().json(JsonLibrary.Jackson)
                .to("direct:uploadAggregatedValidationReport")
                .setBody(header(DATASET_STATUS))
                .to("direct:notifyStatus")
                .to("direct:cleanUpCache")
                .routeId("aggregate-reports");

        from("direct:downloadValidationReport")
                .setHeader(FILE_HANDLE, constant(Constants.BLOBSTORE_PATH_ANTU_WORK)
                        .append(header(DATASET_REFERENTIAL))
                        .append("/")
                        .append(header(VALIDATION_REPORT_ID))
                        .append("/")
                        .append(header(NETEX_FILE_NAME))
                        .append(".json"))
                .log(LoggingLevel.INFO, correlation() + "Downloading Validation Report from GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:getAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Downloaded Validation Report from GCS file ${header." + FILE_HANDLE + "}")
                .routeId("download-validation-report");


        from("direct:uploadAggregatedValidationReport")
                .setHeader(FILE_HANDLE, constant(Constants.BLOBSTORE_PATH_ANTU_REPORTS)
                        .append(header(DATASET_REFERENTIAL))
                        .append("/validation-report-")
                        .append(header(VALIDATION_REPORT_ID))
                        .append(".json"))
                .choice()
                .when(method("antuBlobStoreService", "existBlob"))
                // protection against multiple pubsub message delivery
                .log(LoggingLevel.WARN, correlation() + "The report has already been generated: ${header." + FILE_HANDLE + "}. Ignoring.")
                .stop()
                .otherwise()
                .log(LoggingLevel.INFO, correlation() + "Uploading aggregated Validation Report  to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Uploaded aggregated Validation Report to GCS file ${header." + FILE_HANDLE + "}")
                .routeId("upload-aggregated-validation-report");

        from("direct:cleanUpCache")
                .log(LoggingLevel.INFO, correlation() + "Clean up cache")
                .bean("netexIdRepository", "cleanUp(${header." + VALIDATION_REPORT_ID + "})")
                .bean("temporaryFileRepository", "cleanUp(${header." + VALIDATION_REPORT_ID + "})")
                .log(LoggingLevel.INFO, correlation() + "Cleaned up cache")
                .routeId("cleanup-cache");

    }

    /**
     * Complete the aggregation when all the individual reports have been received.
     * The total number of reports to process is stored in a header that is included in every incoming message.
     */
    private static class ValidationReportAggregationStrategy extends GroupedMessageAggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            Exchange aggregatedExchange = super.aggregate(oldExchange, newExchange);
            aggregatedExchange.getIn().setHeader(VALIDATION_REPORT_ID, newExchange.getIn().getHeader(VALIDATION_REPORT_ID));
            aggregatedExchange.getIn().setHeader(DATASET_CODESPACE, newExchange.getIn().getHeader(DATASET_CODESPACE));
            aggregatedExchange.getIn().setHeader(DATASET_REFERENTIAL, newExchange.getIn().getHeader(DATASET_REFERENTIAL));
            aggregatedExchange.getIn().setHeader(CORRELATION_ID, newExchange.getIn().getHeader(CORRELATION_ID));
            aggregatedExchange.getIn().setHeader(VALIDATION_STAGE_HEADER, newExchange.getIn().getHeader(VALIDATION_STAGE_HEADER));
            aggregatedExchange.getIn().setHeader(VALIDATION_CLIENT_HEADER, newExchange.getIn().getHeader(VALIDATION_CLIENT_HEADER));
            String currentNetexFileNameList = aggregatedExchange.getProperty(PROP_DATASET_NETEX_FILE_NAMES, String.class);
            if (currentNetexFileNameList == null) {
                aggregatedExchange.setProperty(PROP_DATASET_NETEX_FILE_NAMES, newExchange.getIn().getHeader(NETEX_FILE_NAME));
            } else {
                aggregatedExchange.setProperty(PROP_DATASET_NETEX_FILE_NAMES, currentNetexFileNameList + FILENAME_DELIMITER + newExchange.getIn().getHeader(NETEX_FILE_NAME));
            }
            // check if all individual reports have been received
            // checking against the set of distinct file names in order to exclude possible multiple redeliveries of the same report.
            Long nbNetexFiles = newExchange.getIn().getHeader(DATASET_NB_NETEX_FILES, Long.class);
            List<Message> aggregatedMessages = aggregatedExchange.getProperty(ExchangePropertyKey.GROUPED_EXCHANGE, List.class);
            Set<String> aggregatedFileNames = aggregatedMessages.stream().map(message -> message.getHeader(NETEX_FILE_NAME, String.class)).collect(Collectors.toSet());
            if (aggregatedFileNames.size() >= nbNetexFiles) {
                aggregatedExchange.setProperty(Exchange.AGGREGATION_COMPLETE_CURRENT_GROUP, true);
            }
            return aggregatedExchange;
        }
    }

    /**
     * Sort the list in reverse order to get the common files first.
     */
    private static class ReverseSortedFileNameSplitter {

        public static List<String> split(Exchange exchange) {
            String fileNameList = exchange.getMessage().getBody(String.class);
            return Arrays.stream(fileNameList.split(FILENAME_DELIMITER)).sorted(Collections.reverseOrder()).collect(Collectors.toList());
        }
    }
}
