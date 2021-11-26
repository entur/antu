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
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static no.entur.antu.Constants.DATASET_AUTHORITY_ID_VALIDATION_REPORT_ENTRIES;
import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.DATASET_SCHEMA_VALIDATION_REPORT_ENTRIES;
import static no.entur.antu.Constants.DATASET_STREAM;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;


/**
 * Validate NeTEx files.
 */
@Component
public class ValidateFilesRouteBuilder extends BaseRouteBuilder {

    private static final String TIMETABLE_DATASET_FILE = "TIMETABLE_DATASET_FILE";


    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:validateNetex")
                .process(this::setCorrelationIdIfMissing)
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx file ${header." + FILE_HANDLE + "}")

                .doTry()
                .to("direct:downloadSingleNetexFile")
                .log(LoggingLevel.INFO, correlation() + "NeTEx Timetable file downloaded")
                .setHeader(TIMETABLE_DATASET_FILE, body())
                .to("direct:validateNetexDataset")
                .to("direct:saveValidationReport")
                .to("direct:notifyMainJob")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, correlation() + "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}")
                .routeId("validate-netex");

        from("direct:downloadSingleNetexFile")
                .log(LoggingLevel.INFO, correlation() + "Downloading single NeTEx file")
                .to("direct:getAntuBlob")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx file not found")
                .stop()
                //end filter
                .end()
                .routeId("download-single-netex-file");

        from("direct:validateNetexDataset").streamCaching()
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx dataset")
                .setHeader(DATASET_STREAM, body())
                .process(exchange -> {
                    String codespace = exchange.getIn().getHeader(DATASET_CODESPACE, String.class);
                    String validationReportId = exchange.getIn().getHeader(VALIDATION_REPORT_ID, String.class);
                    ValidationReport validationReport = new ValidationReport(codespace, validationReportId);
                    exchange.getIn().setBody(validationReport);
                })
                .setHeader(VALIDATION_REPORT_ID, simple("${body.validationReportId}"))
                .filter(simple("${properties:antu.schema.validation.enabled:true}"))
                .to("direct:validateSchema")
                // end filter
                .end()
                // do not run subsequent validators if the schema validation failed
                .filter(PredicateBuilder.not(simple("${body.hasError()}")))
                .to("direct:validateAuthorityId")
                .end()
                // end filter
                .log(LoggingLevel.INFO, correlation() + "Validated NeTEx dataset")
                .routeId("validate-netex-dataset");

        from("direct:saveValidationReport")
                .log(LoggingLevel.INFO, correlation() + "Saving validation report")
                .marshal().json(JsonLibrary.Jackson)
                .log(LoggingLevel.INFO, correlation() + "Validation report: ${body}")
                .to("direct:uploadValidationReport")
                .log(LoggingLevel.INFO, correlation() + "Saved validation report")
                .routeId("save-validation-report");

        from("direct:uploadValidationReport")
                .setHeader(FILE_HANDLE, constant("work/")
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

        from("direct:validateSchema")
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx schema")
                .setProperty(DATASET_SCHEMA_VALIDATION_REPORT_ENTRIES, method("netexSchemaValidator", "validateSchema(${header." + DATASET_STREAM + "},)"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getIn().getBody(ValidationReport.class);
                    Collection<ValidationReportEntry> netexSchemaValidationReportEntries = exchange.getProperty(DATASET_SCHEMA_VALIDATION_REPORT_ENTRIES, Collection.class);
                    validationReport.addAllValidationReportEntries(netexSchemaValidationReportEntries);
                })
                .log(LoggingLevel.INFO, correlation() + "Validated NeTEx schema")
                .routeId("validate-schema");

        from("direct:validateAuthorityId")
                .log(LoggingLevel.INFO, correlation() + "Validating Authority IDs")
                .setProperty(DATASET_AUTHORITY_ID_VALIDATION_REPORT_ENTRIES, method("authorityIdValidator", "validateAuthorityId(${header." + DATASET_STREAM + "},${header." + DATASET_CODESPACE + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getIn().getBody(ValidationReport.class);
                    Collection<ValidationReportEntry> authorityIdValidationReportEntries = exchange.getProperty(DATASET_AUTHORITY_ID_VALIDATION_REPORT_ENTRIES, Collection.class);
                    validationReport.addAllValidationReportEntries(authorityIdValidationReportEntries);
                })
                .log(LoggingLevel.INFO, correlation() + "Validated Authority IDs")
                .routeId("validate-authority-id");

        from("direct:notifyMainJob")
                .log(LoggingLevel.INFO, correlation() + "Notifying main job")
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuReportAggregationQueue")
                .routeId("notifying main job");

    }
}
