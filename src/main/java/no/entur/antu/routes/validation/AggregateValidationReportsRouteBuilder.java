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
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import no.entur.antu.metrics.AntuPrometheusMetricsService;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.apache.camel.util.StopWatch;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import static no.entur.antu.Constants.TEMPORARY_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_STATUS_SUFFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;


/**
 * Aggregate validation reports.
 */
@Component
public class AggregateValidationReportsRouteBuilder extends BaseRouteBuilder {

    private static final String PROP_DATASET_NETEX_FILE_NAMES = "EnturDatasetNetexFileNames";

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateValidationReportsRouteBuilder.class);

    private static final String PROP_STOP_WATCH = "PROP_STOP_WATCH";
    private final AntuPrometheusMetricsService antuPrometheusMetricsService;

    public AggregateValidationReportsRouteBuilder(AntuPrometheusMetricsService antuPrometheusMetricsService) {
        this.antuPrometheusMetricsService = antuPrometheusMetricsService;
    }

    @Override
    public void configure() throws Exception {
        super.configure();

        from("master:lockOnAntuReportAggregationQueue:google-pubsub:{{antu.pubsub.project.id}}:AntuReportAggregationQueue?concurrentConsumers=10")
                .process(this::removeSynchronizationForAggregatedExchange)
                .aggregate(header(VALIDATION_REPORT_ID_HEADER)).aggregationStrategy(new CollectIndividualReportsAggregationStrategy()).completionTimeout(1800000)
                .process(this::addSynchronizationForAggregatedExchange)
                .log(LoggingLevel.INFO, correlation() + "Aggregated ${exchangeProperty.CamelAggregatedSize} validation reports (aggregation completion triggered by ${exchangeProperty.CamelAggregatedCompletedBy}).")

                .setBody(exchangeProperty(PROP_DATASET_NETEX_FILE_NAMES))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_AGGREGATE_REPORTS))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                .routeId("aggregate-reports-pubsub");

        from("direct:aggregateReports")
                .log(LoggingLevel.INFO, correlation() + "Merging individual reports")
                .setProperty(PROP_STOP_WATCH, StopWatch::new)
                .convertBodyTo(String.class)
                .split(method(ReverseSortedFileNameSplitter.class, "split"))
                    .delimiter(FILENAME_DELIMITER)
                    .aggregationStrategy(new AggregateValidationReportsAggregationStrategy())
                    .log(LoggingLevel.INFO, correlation() + "Merging file ${body}.json")
                    .setHeader(NETEX_FILE_NAME, body())
                    .to("direct:downloadValidationReport")
                    .unmarshal().json(JsonLibrary.Jackson, ValidationReport.class)
                // end splitter
                .end()
                .log(LoggingLevel.INFO, correlation() + "Completed reports merging in ${exchangeProperty." + PROP_STOP_WATCH + ".taken()} ms")
                .choice()
                .when(simple("${body.hasError()}"))
                    .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_FAILED))
                    .log(LoggingLevel.INFO, correlation() + "Validation errors found")
                .otherwise()
                    .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_OK))
                    .log(LoggingLevel.INFO, correlation() + "No validation error")
                    .to("direct:uploadValidationReportMetrics")
                .end()
                .marshal().json(JsonLibrary.Jackson)
                .to("direct:uploadAggregatedValidationReport")
                .setBody(header(DATASET_STATUS))
                .to("direct:notifyStatus")
                .to("direct:createValidationReportStatusFile")
                .to("direct:cleanUpCache")
                .routeId("aggregate-reports");

        from("direct:uploadValidationReportMetrics")
                .bean(antuPrometheusMetricsService)
                .routeId("upload-validation-report-metrics");

        from("direct:downloadValidationReport")
                .setHeader(TEMPORARY_FILE_NAME, constant(Constants.BLOBSTORE_PATH_ANTU_WORK)
                        .append(header(DATASET_REFERENTIAL))
                        .append("/")
                        .append(header(VALIDATION_REPORT_ID_HEADER))
                        .append("/")
                        .append(header(NETEX_FILE_NAME))
                        .append(VALIDATION_REPORT_SUFFIX))
                .log(LoggingLevel.DEBUG, correlation() + "Downloading Validation Report from GCS file ${header." + FILE_HANDLE + "}")
                .doTry()
                .to("direct:downloadBlobFromMemoryStore")
                .doCatch(AntuMemoryStoreFileNotFoundException.class)
                .log(LoggingLevel.WARN, correlation() + "Line validation report ${header." + FILE_HANDLE + "} has already been aggregated and removed from the memory store. Ignoring.")
                .stop()
                .end()
                .log(LoggingLevel.DEBUG, correlation() + "Downloaded Validation Report from GCS file ${header." + FILE_HANDLE + "}")
                .routeId("download-validation-report");


        from("direct:uploadAggregatedValidationReport")
                .setHeader(FILE_HANDLE, constant(Constants.BLOBSTORE_PATH_ANTU_REPORTS)
                        .append(header(DATASET_REFERENTIAL))
                        .append(VALIDATION_REPORT_PREFIX)
                        .append(header(VALIDATION_REPORT_ID_HEADER))
                        .append(VALIDATION_REPORT_STATUS_SUFFIX))
                .choice()
                .when(method("antuBlobStoreService", "existBlob"))
                // protection against multiple pubsub message delivery
                .log(LoggingLevel.WARN, correlation() + "The report has already been generated: ${header." + FILE_HANDLE + "}. Ignoring.")
                .stop()
                .otherwise()
                .setHeader(FILE_HANDLE, constant(Constants.BLOBSTORE_PATH_ANTU_REPORTS)
                        .append(header(DATASET_REFERENTIAL))
                        .append(VALIDATION_REPORT_PREFIX)
                        .append(header(VALIDATION_REPORT_ID_HEADER))
                        .append(VALIDATION_REPORT_SUFFIX))
                .log(LoggingLevel.INFO, correlation() + "Uploading aggregated Validation Report  to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Uploaded aggregated Validation Report to GCS file ${header." + FILE_HANDLE + "}")
                .routeId("upload-aggregated-validation-report");

        from("direct:createValidationReportStatusFile")
                // Create a status file after the PubSub status notification is sent to Marduk.
                // This covers the case where the application process crashes or is restarted between the time
                // the aggregated report is uploaded and the time the PubSub notification message is sent.
                // When the PubSub message in the job queue is retried, the validation report should be uploaded and
                // the notification sent only if the status file is missing.
                .log(LoggingLevel.INFO, correlation() + "Create validation report status file")
                .setBody(constant("OK"))
                .setHeader(FILE_HANDLE, constant(Constants.BLOBSTORE_PATH_ANTU_REPORTS)
                        .append(header(DATASET_REFERENTIAL))
                        .append(VALIDATION_REPORT_PREFIX)
                        .append(header(VALIDATION_REPORT_ID_HEADER))
                        .append(VALIDATION_REPORT_STATUS_SUFFIX))
                .to("direct:uploadAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Created validation report status file")
                .routeId("create-validation-report-status-file");

        from("direct:cleanUpCache")
                .log(LoggingLevel.INFO, correlation() + "Clean up cache")
                .bean("netexIdRepository", "cleanUp(${header." + VALIDATION_REPORT_ID_HEADER + "})")
                .bean("temporaryFileRepository", "cleanUp(${header." + VALIDATION_REPORT_ID_HEADER + "})")
                .bean("swedenStopPlaceNetexIdRepository", "cleanUp(${header." + VALIDATION_REPORT_ID_HEADER + "})")
                .log(LoggingLevel.INFO, correlation() + "Cleaned up cache")
                .routeId("cleanup-cache");

    }

    private static class AggregateValidationReportsAggregationStrategy extends GroupedMessageAggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }

            ValidationReport oldValidationReport = oldExchange.getIn().getBody(ValidationReport.class);
            ValidationReport newValidationReport = newExchange.getIn().getBody(ValidationReport.class);

            List<ValidationReportEntry> validationReportEntries = Stream.concat(
                    oldValidationReport.getValidationReportEntries().stream(),
                    newValidationReport.getValidationReportEntries().stream()
            ).toList();

            Map<String, Long> numberOfValidationEntriesPerRule = Stream.concat(
                    oldValidationReport.getNumberOfValidationEntriesPerRule().entrySet().stream(),
                    newValidationReport.getNumberOfValidationEntriesPerRule().entrySet().stream()
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum));

            oldExchange.getIn().setBody(
                    new ValidationReport(
                            oldExchange.getIn().getHeader(DATASET_CODESPACE, String.class),
                            oldExchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER, String.class),
                            validationReportEntries,
                            numberOfValidationEntriesPerRule));

            return oldExchange;
        }
    }

    /**
     * Complete the aggregation when all the individual reports have been received.
     * The total number of reports to process is stored in a header that is included in every incoming message.
     */
    private static class CollectIndividualReportsAggregationStrategy extends AntuGroupedMessageAggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            Exchange aggregatedExchange = super.aggregate(oldExchange, newExchange);
            copyValidationHeaders(newExchange, aggregatedExchange);
            String currentNetexFileNameList = aggregatedExchange.getProperty(PROP_DATASET_NETEX_FILE_NAMES, String.class);
            String incomingNetexFileName = newExchange.getIn().getHeader(NETEX_FILE_NAME, String.class);
            if (currentNetexFileNameList == null) {
                aggregatedExchange.setProperty(PROP_DATASET_NETEX_FILE_NAMES, incomingNetexFileName);
            } else {
                aggregatedExchange.setProperty(PROP_DATASET_NETEX_FILE_NAMES, currentNetexFileNameList + FILENAME_DELIMITER + incomingNetexFileName);
            }
            // check if all individual reports have been received
            // checking against the set of distinct file names in order to exclude possible multiple redeliveries of the same report.
            Long nbNetexFiles = newExchange.getIn().getHeader(DATASET_NB_NETEX_FILES, Long.class);
            List<Message> aggregatedMessages = aggregatedExchange.getProperty(ExchangePropertyKey.GROUPED_EXCHANGE, List.class);
            Set<String> aggregatedFileNames = aggregatedMessages.stream().map(message -> message.getHeader(NETEX_FILE_NAME, String.class)).collect(Collectors.toSet());

            if (LOGGER.isTraceEnabled()) {
                String reportId = aggregatedExchange.getIn().getHeader(VALIDATION_REPORT_ID_HEADER, String.class);
                String receivedFileNames = Arrays.stream(aggregatedExchange.getProperty(PROP_DATASET_NETEX_FILE_NAMES, String.class).split(FILENAME_DELIMITER)).sorted().collect(Collectors.joining(FILENAME_DELIMITER));
                LOGGER.trace("Received file {} for report {}. All received files: {}", incomingNetexFileName, reportId, receivedFileNames);
            }

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
            return Arrays.stream(fileNameList.split(FILENAME_DELIMITER)).sorted(Collections.reverseOrder()).toList();
        }
    }
}
