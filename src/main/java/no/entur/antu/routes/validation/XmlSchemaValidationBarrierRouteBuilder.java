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
import static no.entur.antu.util.NetexFileUtils.buildFileNamesListFromString;

import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.util.StopWatch;
import org.entur.netex.validation.validator.ValidationReport;
import org.springframework.stereotype.Component;

/**
 * Barrier waiting for all files to be validated against the NeTEx XML Schema before running further validation.
 */
@Component
public class XmlSchemaValidationBarrierRouteBuilder extends BaseRouteBuilder {

  @Override
  public void configure() throws Exception {
    super.configure();

    from(
      "master:lockOnAntuXmlSchemaValidationAggregationQueue:google-pubsub:{{antu.pubsub.project.id}}:AntuXmlSchemaValidationAggregationQueue"
    )
      .process(this::removeSynchronizationForAggregatedExchange)
      .aggregate(header(VALIDATION_REPORT_ID_HEADER))
      .aggregationStrategy(new CollectIndividualReportsAggregationStrategy())
      .completionTimeout(1800000)
      .process(this::addSynchronizationForAggregatedExchange)
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Aggregated ${exchangeProperty.CamelAggregatedSize} individual reports after XML Schema validation (aggregation completion triggered by ${exchangeProperty.CamelAggregatedCompletedBy})."
      )
      .setBody(exchangeProperty(PROP_DATASET_NETEX_FILE_NAMES_STRING))
      .log(LoggingLevel.TRACE, correlation() + "All NeTEx Files: ${body}")
      .setHeader(
        Constants.JOB_TYPE,
        simple(JOB_TYPE_AGGREGATE_XML_SCHEMA_VALIDATION)
      )
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
      .routeId("aggregate-xml-schema-validation-pubsub");

    from("direct:aggregateXmlSchemaValidation")
      .log(
        LoggingLevel.INFO,
        correlation() + "All XML Schema validation reports received"
      )
      .setProperty(PROP_STOP_WATCH, StopWatch::new)
      .process(exchange ->
        exchange.setProperty(
          PROP_DATASET_NETEX_FILE_NAMES_SET,
          buildFileNamesListFromString(exchange.getIn().getBody(String.class))
        )
      )
      .convertBodyTo(String.class)
      .split(method(ReverseSortedFileNameSplitter.class, "split"))
      .delimiter(FILENAME_DELIMITER)
      .aggregationStrategy(new AggregateValidationReportsAggregationStrategy())
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Merging XML schema validation report for file ${body}.json"
      )
      .setHeader(NETEX_FILE_NAME, body())
      .to("direct:downloadValidationReport")
      .unmarshal()
      .json(JsonLibrary.Jackson, ValidationReport.class)
      // end splitter
      .end()
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Completed XML schema validation reports merging in ${exchangeProperty." +
        PROP_STOP_WATCH +
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
      .to("direct:validateCommonFiles")
      .end()
      .routeId("aggregate-xml-schema-validation");
  }
}
