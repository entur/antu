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

import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.DATASET_STATUS;
import static no.entur.antu.Constants.FILENAME_DELIMITER;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.JOB_TYPE_AGGREGATE_REPORTS;
import static no.entur.antu.Constants.JOB_TYPE_VALIDATE_DATASET;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.REPORT_CREATION_DATE;
import static no.entur.antu.Constants.STATUS_VALIDATION_FAILED;
import static no.entur.antu.Constants.STATUS_VALIDATION_OK;
import static no.entur.antu.Constants.TEMPORARY_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_PROFILE_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_STATUS_SUFFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;

import java.time.LocalDateTime;
import no.entur.antu.Constants;
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import no.entur.antu.metrics.AntuPrometheusMetricsService;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.validation.AntuNetexValidationProgressCallback;
import no.entur.antu.validation.NetexValidationWorkflow;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.util.StopWatch;
import org.entur.netex.validation.validator.ValidationReport;
import org.springframework.stereotype.Component;

/**
 * Aggregate validation reports.
 */
@Component
public class AggregateValidationReportsRouteBuilder extends BaseRouteBuilder {

  private static final String PROP_HAS_DATASET_VALIDATORS =
    "HAS_DATASET_VALIDATORS";

  private final NetexValidationWorkflow netexValidationWorkflow;
  private final AntuPrometheusMetricsService antuPrometheusMetricsService;

  public AggregateValidationReportsRouteBuilder(
    NetexValidationWorkflow netexValidationWorkflow,
    AntuPrometheusMetricsService antuPrometheusMetricsService
  ) {
    this.netexValidationWorkflow = netexValidationWorkflow;
    this.antuPrometheusMetricsService = antuPrometheusMetricsService;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    from(
      "master:lockOnAntuReportAggregationQueue:google-pubsub:{{antu.pubsub.project.id}}:AntuReportAggregationQueue?concurrentConsumers=10"
    )
      .process(this::removeSynchronizationForAggregatedExchange)
      .aggregate(header(VALIDATION_REPORT_ID_HEADER))
      .aggregationStrategy(new CollectIndividualReportsAggregationStrategy())
      .completionTimeout(1800000)
      .process(this::addSynchronizationForAggregatedExchange)
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Aggregated ${exchangeProperty.CamelAggregatedSize} validation reports (aggregation completion triggered by ${exchangeProperty.CamelAggregatedCompletedBy})."
      )
      .setBody(exchangeProperty(Constants.PROP_DATASET_NETEX_FILE_NAMES_STRING))
      .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_AGGREGATE_REPORTS))
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
      .routeId("aggregate-reports-pubsub");

    from("direct:aggregateReports")
      .log(LoggingLevel.INFO, correlation() + "Merging individual reports")
      .setProperty(Constants.PROP_STOP_WATCH, StopWatch::new)
      .convertBodyTo(String.class)
      .split(method(ReverseSortedFileNameSplitter.class, "split"))
      .delimiter(FILENAME_DELIMITER)
      .aggregationStrategy(new AggregateValidationReportsAggregationStrategy())
      .log(LoggingLevel.DEBUG, correlation() + "Merging file ${body}.json")
      .setHeader(NETEX_FILE_NAME, body())
      .to("direct:downloadValidationReport")
      .unmarshal()
      .json(JsonLibrary.Jackson, ValidationReport.class)
      // end splitter
      .end()
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Completed reports merging in ${exchangeProperty." +
        Constants.PROP_STOP_WATCH +
        ".taken()} ms"
      )
      .choice()
      .when(simple("${body.hasError()}"))
      .log(
        LoggingLevel.INFO,
        correlation() + "Validation errors found, skipping dataset validation"
      )
      .to("direct:completeValidation")
      .otherwise()
      .marshal()
      .json(JsonLibrary.Jackson)
      .to("direct:uploadValidationReport")
      .setBody(header(NETEX_FILE_NAME))
      .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_VALIDATE_DATASET))
      .convertHeaderTo(REPORT_CREATION_DATE, String.class)
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
      .end()
      .routeId("aggregate-reports");

    from("direct:validateDataset")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Downloading the aggregated validation report for dataset validation"
      )
      .convertBodyTo(String.class)
      .setHeader(NETEX_FILE_NAME, body())
      .to("direct:downloadValidationReport")
      .unmarshal()
      .json(JsonLibrary.Jackson, ValidationReport.class)
      .process(exchange ->
        exchange.setProperty(
          Constants.PROP_NETEX_VALIDATION_CALLBACK,
          new AntuNetexValidationProgressCallback(this, exchange)
        )
      )
      .process(e -> {
        boolean hasDatasetValidator =
          netexValidationWorkflow.hasDatasetValidators(
            e.getIn().getHeader(VALIDATION_PROFILE_HEADER, String.class)
          );
        e.setProperty(PROP_HAS_DATASET_VALIDATORS, hasDatasetValidator);
      })
      .filter(exchangeProperty(PROP_HAS_DATASET_VALIDATORS).isEqualTo(true))
      .bean(
        "netexValidationWorkflow",
        "runDatasetValidators(" +
        "${body}, " +
        "${header." +
        VALIDATION_PROFILE_HEADER +
        "},${exchangeProperty." +
        Constants.PROP_NETEX_VALIDATION_CALLBACK +
        "})"
      )
      .log(
        LoggingLevel.INFO,
        correlation() + "Completed all NeTEx dataset validators"
      )
      //end filter
      .end()
      .to("direct:completeValidation")
      .routeId("validate-dataset");

    from("direct:completeValidation")
      .choice()
      .when(simple("${body.hasError()}"))
      .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_FAILED))
      .log(LoggingLevel.INFO, correlation() + "Validation errors found")
      .otherwise()
      .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_OK))
      .log(LoggingLevel.INFO, correlation() + "No validation error")
      .to("direct:uploadValidationReportMetrics")
      .end()
      .marshal()
      .json(JsonLibrary.Jackson)
      .to("direct:uploadAggregatedValidationReport")
      .setBody(header(DATASET_STATUS))
      .to("direct:notifyStatus")
      .to("direct:createValidationReportStatusFile")
      .to("direct:cleanUpCache")
      .routeId("complete-validation");

    from("direct:uploadValidationReportMetrics")
      .convertHeaderTo(REPORT_CREATION_DATE, LocalDateTime.class)
      .bean(antuPrometheusMetricsService)
      .routeId("upload-validation-report-metrics");

    from("direct:downloadValidationReport")
      .setHeader(
        TEMPORARY_FILE_NAME,
        constant(Constants.BLOBSTORE_PATH_ANTU_WORK)
          .append(header(DATASET_REFERENTIAL))
          .append("/")
          .append(header(VALIDATION_REPORT_ID_HEADER))
          .append("/")
          .append(header(NETEX_FILE_NAME))
          .append(VALIDATION_REPORT_SUFFIX)
      )
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Downloading Validation Report from GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .doTry()
      .to("direct:downloadBlobFromMemoryStore")
      .doCatch(AntuMemoryStoreFileNotFoundException.class)
      .log(
        LoggingLevel.WARN,
        correlation() +
        "Line validation report ${header." +
        FILE_HANDLE +
        "} has already been aggregated and removed from the memory store. Ignoring."
      )
      .stop()
      .end()
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Downloaded Validation Report from GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .routeId("download-validation-report");

    from("direct:uploadAggregatedValidationReport")
      .setHeader(
        FILE_HANDLE,
        constant(Constants.BLOBSTORE_PATH_ANTU_REPORTS)
          .append(header(DATASET_REFERENTIAL))
          .append(VALIDATION_REPORT_PREFIX)
          .append(header(VALIDATION_REPORT_ID_HEADER))
          .append(VALIDATION_REPORT_STATUS_SUFFIX)
      )
      .choice()
      .when(method("antuBlobStoreService", "existBlob"))
      // protection against multiple pubsub message delivery
      .log(
        LoggingLevel.WARN,
        correlation() +
        "The report has already been generated: ${header." +
        FILE_HANDLE +
        "}. Ignoring."
      )
      .stop()
      .otherwise()
      .setHeader(
        FILE_HANDLE,
        constant(Constants.BLOBSTORE_PATH_ANTU_REPORTS)
          .append(header(DATASET_REFERENTIAL))
          .append(VALIDATION_REPORT_PREFIX)
          .append(header(VALIDATION_REPORT_ID_HEADER))
          .append(VALIDATION_REPORT_SUFFIX)
      )
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploading aggregated Validation Report  to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .to("direct:uploadAntuBlob")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Uploaded aggregated Validation Report to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .routeId("upload-aggregated-validation-report");

    from("direct:createValidationReportStatusFile")
      // Create a status file after the PubSub status notification is sent to Marduk.
      // This covers the case where the application process crashes or is restarted between the time
      // the aggregated report is uploaded and the time the PubSub notification message is sent.
      // When the PubSub message in the job queue is retried, the validation report should be uploaded and
      // the notification sent only if the status file is missing.
      .log(
        LoggingLevel.INFO,
        correlation() + "Create validation report status file"
      )
      .setBody(constant("OK"))
      .setHeader(
        FILE_HANDLE,
        constant(Constants.BLOBSTORE_PATH_ANTU_REPORTS)
          .append(header(DATASET_REFERENTIAL))
          .append(VALIDATION_REPORT_PREFIX)
          .append(header(VALIDATION_REPORT_ID_HEADER))
          .append(VALIDATION_REPORT_STATUS_SUFFIX)
      )
      .to("direct:uploadAntuBlob")
      .log(
        LoggingLevel.INFO,
        correlation() + "Created validation report status file"
      )
      .routeId("create-validation-report-status-file");

    from("direct:cleanUpCache")
      .log(LoggingLevel.INFO, correlation() + "Clean up cache")
      .bean(
        "netexIdRepository",
        "cleanUp(${header." + VALIDATION_REPORT_ID_HEADER + "})"
      )
      .bean(
        "temporaryFileRepository",
        "cleanUp(${header." + VALIDATION_REPORT_ID_HEADER + "})"
      )
      .bean(
        "swedenStopPlaceNetexIdRepository",
        "cleanUp(${header." + VALIDATION_REPORT_ID_HEADER + "})"
      )
      .bean(
        "netexDataRepository",
        "cleanUp(${header." + VALIDATION_REPORT_ID_HEADER + "})"
      )
      .log(LoggingLevel.INFO, correlation() + "Cleaned up cache")
      .routeId("cleanup-cache");
  }
}
