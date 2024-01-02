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

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_WORK;
import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.TEMPORARY_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_PROFILE_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;
import static no.entur.antu.routes.memorystore.MemoryStoreRoute.MEMORY_STORE_FILE_NAME;

import no.entur.antu.exception.AntuException;
import no.entur.antu.exception.RetryableAntuException;
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.validator.AntuNetexValidationProgressCallback;
import no.entur.antu.validator.ValidationReportTransformer;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.util.StopWatch;
import org.entur.netex.validation.exception.RetryableNetexValidationException;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.springframework.stereotype.Component;

/**
 * Validate NeTEx files, both common files and line files.
 */
@Component
public class ValidateFilesRouteBuilder extends BaseRouteBuilder {

  private static final String PROP_NETEX_FILE_CONTENT = "NETEX_FILE_CONTENT";
  private static final String PROP_ALL_NETEX_FILE_NAMES =
    "ALL_NETEX_FILE_NAMES";
  private static final String PROP_STOP_WATCH = "PROP_STOP_WATCH";
  private static final String PROP_NETEX_VALIDATION_CALLBACK =
    "PROP_NETEX_VALIDATION_CALLBACK";

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:validateNetex")
      .log(
        LoggingLevel.INFO,
        correlation() + "Validating NeTEx file ${header." + FILE_HANDLE + "}"
      )
      .process(this::extendAckDeadline)
      .setProperty(PROP_STOP_WATCH, StopWatch::new)
      .setProperty(PROP_ALL_NETEX_FILE_NAMES, body())
      .doTry()
      .setHeader(MEMORY_STORE_FILE_NAME, header(NETEX_FILE_NAME))
      .to("direct:downloadSingleNetexFileFromMemoryStore")
      .setProperty(PROP_NETEX_FILE_CONTENT, body())
      .to("direct:runNetexValidators")
      // Duplicated PubSub messages are detected when trying to download the NeTEx file: it does not exist anymore after the report is generated and all temporary files are deleted
      .doCatch(AntuMemoryStoreFileNotFoundException.class)
      .log(
        LoggingLevel.WARN,
        correlation() +
        "NeTEx file ${header." +
        FILE_HANDLE +
        "} has already been validated and removed from the memory store. Ignoring."
      )
      .stop()
      .doCatch(
        InterruptedException.class,
        RetryableNetexValidationException.class,
        RetryableAntuException.class
      )
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Retryable exception while processing file ${header." +
        FILE_HANDLE +
        "}, the file will be retried later: ${exception.message} stacktrace: ${exception.stacktrace}"
      )
      .throwException(new AntuException("File processing interrupted"))
      .doCatch(Exception.class)
      .log(
        LoggingLevel.ERROR,
        correlation() +
        "System error while validating the NeTEx file ${header." +
        FILE_HANDLE +
        "}: ${exception.message} stacktrace: ${exception.stacktrace}"
      )
      .to("direct:reportSystemError")
      // end catch
      .end()
      .to("direct:truncateReport")
      .to("direct:saveValidationReport")
      .to("direct:notifyValidationReportAggregator")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Validated NeTEx file ${header." +
        NETEX_FILE_NAME +
        "} in ${exchangeProperty." +
        PROP_STOP_WATCH +
        ".taken()} ms"
      )
      .routeId("validate-netex");

    from("direct:runNetexValidators")
      .streamCaching()
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Running NeTEx validators using validation profile ${header." +
        VALIDATION_PROFILE_HEADER +
        "}"
      )
      .validate(header(VALIDATION_PROFILE_HEADER).isNotNull())
      .validate(header(DATASET_CODESPACE).isNotNull())
      .process(exchange ->
        exchange.setProperty(
          PROP_NETEX_VALIDATION_CALLBACK,
          new AntuNetexValidationProgressCallback(this, exchange)
        )
      )
      .bean(
        "netexValidationProfile",
        "validate(${header." +
        VALIDATION_PROFILE_HEADER +
        "}, ${header." +
        DATASET_CODESPACE +
        "},${header." +
        VALIDATION_REPORT_ID_HEADER +
        "},${header." +
        NETEX_FILE_NAME +
        "},${exchangeProperty." +
        PROP_NETEX_FILE_CONTENT +
        "},${exchangeProperty." +
        PROP_NETEX_VALIDATION_CALLBACK +
        "})"
      )
      .log(LoggingLevel.DEBUG, correlation() + "Completed all NeTEx validators")
      .routeId("run-netex-validators");

    from("direct:reportSystemError")
      .process(exchange -> {
        String codespace = exchange
          .getIn()
          .getHeader(DATASET_CODESPACE, String.class);
        String validationReportId = exchange
          .getIn()
          .getHeader(VALIDATION_REPORT_ID_HEADER, String.class);
        ValidationReport validationReport = new ValidationReport(
          codespace,
          validationReportId
        );
        String fileName = exchange
          .getIn()
          .getHeader(NETEX_FILE_NAME, String.class);
        ValidationReportEntry validationReportEntry = new ValidationReportEntry(
          "System error while validating the file " +
          exchange.getIn().getHeader(NETEX_FILE_NAME),
          "SYSTEM_ERROR",
          ValidationReportEntrySeverity.ERROR,
          new DataLocation(null, fileName, null, null)
        );
        validationReport.addValidationReportEntry(validationReportEntry);
        exchange.getIn().setBody(validationReport, ValidationReport.class);
      })
      .routeId("report-system-error");

    from("direct:truncateReport")
      .log(LoggingLevel.DEBUG, correlation() + "Truncating validation report")
      .bean(new ValidationReportTransformer(50))
      .log(LoggingLevel.DEBUG, correlation() + "Truncated validation report")
      .routeId("truncate-validation-report");

    from("direct:saveValidationReport")
      .log(LoggingLevel.DEBUG, correlation() + "Saving validation report")
      .marshal()
      .json(JsonLibrary.Jackson)
      .to("direct:uploadValidationReport")
      .log(LoggingLevel.DEBUG, correlation() + "Saved validation report")
      .routeId("save-validation-report");

    from("direct:uploadValidationReport")
      .setHeader(
        TEMPORARY_FILE_NAME,
        constant(BLOBSTORE_PATH_ANTU_WORK)
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
        "Uploading Validation Report  to GCS file ${header." +
        TEMPORARY_FILE_NAME +
        "}"
      )
      .to("direct:uploadBlobToMemoryStore")
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Uploaded Validation Report to GCS file ${header." +
        TEMPORARY_FILE_NAME +
        "}"
      )
      .routeId("upload-validation-report");

    from("direct:notifyValidationReportAggregator")
      .log(
        LoggingLevel.DEBUG,
        correlation() + "Notifying validation report aggregator"
      )
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuReportAggregationQueue")
      .filter(header(NETEX_FILE_NAME).startsWith("_"))
      .log(
        LoggingLevel.DEBUG,
        correlation() + "Notifying common files aggregator"
      )
      .setBody(exchangeProperty(PROP_ALL_NETEX_FILE_NAMES))
      .to(
        "google-pubsub:{{antu.pubsub.project.id}}:AntuCommonFilesAggregationQueue"
      )
      //end filter
      .end()
      .routeId("notify-validation-report-aggregator");
  }
}
