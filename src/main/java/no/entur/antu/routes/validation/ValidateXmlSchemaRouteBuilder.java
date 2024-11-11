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

import static no.entur.antu.Constants.*;
import static no.entur.antu.routes.memorystore.MemoryStoreRoute.MEMORY_STORE_FILE_NAME;

import java.util.Set;
import no.entur.antu.Constants;
import no.entur.antu.exception.AntuException;
import no.entur.antu.exception.RetryableAntuException;
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.util.NetexFileUtils;
import no.entur.antu.validation.AntuNetexValidationProgressCallback;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.util.StopWatch;
import org.entur.netex.validation.exception.RetryableNetexValidationException;
import org.springframework.stereotype.Component;

/**
 * Validate NeTEx files against the NeTEx XML Schema.
 */
@Component
public class ValidateXmlSchemaRouteBuilder extends BaseRouteBuilder {

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:createXMLSchemaValidationJobs")
      .log(
        LoggingLevel.DEBUG,
        correlation() + "Creating XML Schema validation jobs"
      )
      .split(exchangeProperty(PROP_DATASET_NETEX_FILE_NAMES_SET))
      .setHeader(
        Constants.DATASET_NB_NETEX_FILES,
        exchangeProperty(Exchange.SPLIT_SIZE)
      )
      .setHeader(NETEX_FILE_NAME, body())
      .setHeader(FILE_HANDLE, simple(Constants.GCS_BUCKET_FILE_NAME))
      .process(exchange ->
        exchange
          .getIn()
          .setBody(
            NetexFileUtils.buildFileNamesListFromSet(
              exchange.getProperty(PROP_DATASET_NETEX_FILE_NAMES_SET, Set.class)
            )
          )
      )
      .log(LoggingLevel.TRACE, correlation() + "All NeTEx Files: ${body}")
      .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_VALIDATE_XML_SCHEMA))
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
      //end split
      .end()
      .routeId("create-xml-schema-validation-jobs");

    from("direct:validateXmlSchema")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Validating XML schema for NeTEx file ${header." +
        FILE_HANDLE +
        "}"
      )
      .process(this::extendAckDeadline)
      .setProperty(PROP_STOP_WATCH, StopWatch::new)
      .setProperty(PROP_DATASET_NETEX_FILE_NAMES_SET, body())
      .doTry()
      .setHeader(MEMORY_STORE_FILE_NAME, header(NETEX_FILE_NAME))
      .to("direct:downloadSingleNetexFileFromMemoryStore")
      .setProperty(PROP_NETEX_FILE_CONTENT, body())
      .to("direct:runXmlSchemaValidator")
      // Duplicated PubSub messages are detected when trying to download the NeTEx file:
      // it does not exist anymore after the report is generated and all temporary files are deleted
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
      .to("direct:saveValidationReport")
      .to("direct:notifyXmlSchemaValidationAggregator")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Validated XML Schema for NeTEx file ${header." +
        NETEX_FILE_NAME +
        "} in ${exchangeProperty." +
        PROP_STOP_WATCH +
        ".taken()} ms"
      )
      .routeId("validate-xml-schema");

    from("direct:runXmlSchemaValidator")
      .streamCaching()
      .log(LoggingLevel.DEBUG, correlation() + "Running XML Schema validator")
      .validate(header(DATASET_CODESPACE).isNotNull())
      .process(exchange ->
        exchange.setProperty(
          PROP_NETEX_VALIDATION_CALLBACK,
          new AntuNetexValidationProgressCallback(this, exchange)
        )
      )
      .bean(
        "netexValidationWorkflow",
        "runSchemaValidation(${header." +
        VALIDATION_PROFILE_HEADER +
        "}, ${header." +
        DATASET_CODESPACE +
        "},${header." +
        VALIDATION_REPORT_ID_HEADER +
        "},${header." +
        NETEX_FILE_NAME +
        "},${exchangeProperty." +
        PROP_NETEX_FILE_CONTENT +
        "}, " +
        "${exchangeProperty." +
        PROP_NETEX_VALIDATION_CALLBACK +
        "})"
      )
      .log(
        LoggingLevel.DEBUG,
        correlation() + "Completed NeTEx XML Schema validation"
      )
      .routeId("run-xml-schema-validator");

    from("direct:notifyXmlSchemaValidationAggregator")
      .log(
        LoggingLevel.DEBUG,
        correlation() + "Notifying XML Schema validation report aggregator"
      )
      .to(
        "google-pubsub:{{antu.pubsub.project.id}}:AntuXmlSchemaValidationAggregationQueue"
      )
      .routeId("notify-xml-schema-validation-aggregator");
  }
}
