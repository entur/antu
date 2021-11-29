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


import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.validator.ValidationReport;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_WORK;
import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;


/**
 * Validate NeTEx files.
 */
@Component
public class ValidateFilesRouteBuilder extends BaseRouteBuilder {

    private static final String PROP_NETEX_FILE_CONTENT = "NETEX_FILE_CONTENT";
    private static final String PROP_VALIDATION_REPORT = "VALIDATION_REPORT";

    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:validateNetex")
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx file ${header." + FILE_HANDLE + "}")
                .to("direct:initValidationReport")
                .doTry()
                .to("direct:downloadSingleNetexFile")
                .setProperty(PROP_NETEX_FILE_CONTENT, body())
                .to("direct:runNetexValidators")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, correlation() + "System error while validating the NeTEx file ${header." + FILE_HANDLE + "}: ${exception.message} stacktrace: ${exception.stacktrace}")
                .to("direct:reportSystemError")
                // end catch
                .end()
                .to("direct:saveValidationReport")
                .to("direct:notifyValidationReportAggregator")
                .routeId("validate-netex");

        from("direct:initValidationReport")
                .process(exchange -> {
                    String codespace = exchange.getIn().getHeader(DATASET_CODESPACE, String.class);
                    String validationReportId = exchange.getIn().getHeader(VALIDATION_REPORT_ID, String.class);
                    ValidationReport validationReport = new ValidationReport(codespace, validationReportId);
                    exchange.setProperty(PROP_VALIDATION_REPORT, validationReport);
                })
                .setHeader(VALIDATION_REPORT_ID, simple("${exchangeProperty." + PROP_VALIDATION_REPORT + ".validationReportId}"))
                .routeId("init-validation-report");

        from("direct:downloadSingleNetexFile")
                .log(LoggingLevel.INFO, correlation() + "Downloading single NeTEx file ${header." + FILE_HANDLE + "}")
                .to("direct:getAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Downloaded single NeTEx file ${header." + FILE_HANDLE + "}")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx file not found: ${header." + FILE_HANDLE + "}")
                .stop()
                //end filter
                .end()
                .routeId("download-single-netex-file");

        from("direct:runNetexValidators").streamCaching()
                .log(LoggingLevel.INFO, correlation() + "Running NeTEx validators")
                .filter(simple("${properties:antu.schema.validation.enabled:true}"))
                .to("direct:validateSchema")
                // end filter
                .end()
                // do not run subsequent validators if the schema validation failed
                .filter(PredicateBuilder.not(simple("${exchangeProperty." + PROP_VALIDATION_REPORT + ".hasError()}")))
                .to("direct:validateAuthorityId")
                .end()
                // end filter
                .log(LoggingLevel.INFO, correlation() + "Completed all NeTEx validators")
                .routeId("run-netex-validators");

        from("direct:validateSchema")
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx schema")
                .setBody(method("netexSchemaValidator", "validateSchema(${exchangeProperty." + PROP_NETEX_FILE_CONTENT + "},)"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "Validated NeTEx schema")
                .routeId("validate-schema");

        from("direct:validateAuthorityId")
                .log(LoggingLevel.INFO, correlation() + "Validating Authority IDs")
                .setBody(method("authorityIdValidator", "validateAuthorityId(${exchangeProperty." + PROP_NETEX_FILE_CONTENT + "},${header." + DATASET_CODESPACE + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "Validated Authority IDs")
                .routeId("validate-authority-id");

        from("direct:reportSystemError")
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    ValidationReportEntry validationReportEntry = new ValidationReportEntry("System error while validating the  file " + exchange.getIn().getHeader(NETEX_FILE_NAME), "System error", ValidationReportEntrySeverity.ERROR);
                    validationReport.addValidationReportEntry(validationReportEntry);
                })
                .routeId("report-system-error");

        from("direct:saveValidationReport")
                .log(LoggingLevel.INFO, correlation() + "Saving validation report")
                .setBody(exchangeProperty(PROP_VALIDATION_REPORT))
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, correlation() + "Validation report: ${body}")
                .to("direct:uploadValidationReport")
                .log(LoggingLevel.INFO, correlation() + "Saved validation report")
                .routeId("save-validation-report");

        from("direct:uploadValidationReport")
                .setHeader(FILE_HANDLE, constant(BLOBSTORE_PATH_ANTU_WORK)
                        .append(header(DATASET_CODESPACE))
                        .append("/")
                        .append(header(VALIDATION_REPORT_ID))
                        .append("/")
                        .append(header(NETEX_FILE_NAME))
                        .append(".json"))
                .log(LoggingLevel.INFO, correlation() + "Uploading Validation Report  to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Uploaded Validation Report to GCS file ${header." + FILE_HANDLE + "}")
                .routeId("upload-validation-report");

        from("direct:notifyValidationReportAggregator")
                .log(LoggingLevel.INFO, correlation() + "Notifying validation report aggregator")
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuReportAggregationQueue")
                .routeId("notify-validation-report-aggregator");

    }
}
