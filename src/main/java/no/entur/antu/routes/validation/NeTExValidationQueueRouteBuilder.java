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
import static no.entur.antu.Constants.DATASET_STATUS;
import static no.entur.antu.Constants.DATASET_STREAM;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;


/**
 * Validate a NeTEx file upon notification from Marduk..
 */
@Component
public class NeTExValidationQueueRouteBuilder extends BaseRouteBuilder {

    private static final String TIMETABLE_DATASET_FILE = "TIMETABLE_DATASET_FILE";

    public static final String STATUS_VALIDATION_STARTED = "started";
    public static final String STATUS_VALIDATION_OK = "ok";
    public static final String STATUS_VALIDATION_FAILED = "failed";

    @Override
    public void configure() throws Exception {
        super.configure();


        from("google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationQueue")
                .to("direct:netexValidationQueue")
                .routeId("netex-validation-queue-pubsub");

        from("direct:netexValidationQueue")
                .process(this::setCorrelationIdIfMissing)
                .log(LoggingLevel.INFO, correlation() + "Received NeTEx validation request")

                .setBody(constant(STATUS_VALIDATION_STARTED))
                .to("direct:notifyMarduk")

                .doTry()
                .to("direct:downloadNetexDataset")
                .log(LoggingLevel.INFO, correlation() + "NeTEx Timetable file downloaded")
                .setHeader(TIMETABLE_DATASET_FILE, body())
                .to("direct:validateNetexDataset")
                .to("direct:saveValidationReport")
                .setBody(header(DATASET_STATUS))
                .to("direct:notifyMarduk")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, correlation() + "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}")
                .setBody(constant(STATUS_VALIDATION_FAILED))
                .to("direct:notifyMarduk")
                .routeId("netex-validation-queue");

        from("direct:downloadNetexDataset")
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx dataset")
                .to("direct:getMardukBlob")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx file not found")
                .stop()
                //end filter
                .end()
                .routeId("download-netex-timetable-dataset");

        from("direct:validateNetexDataset").streamCaching()
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx dataset")
                .setHeader(DATASET_STREAM, body())
                .process(exchange -> {
                    String codespace = exchange.getIn().getHeader(DATASET_CODESPACE, String.class);
                    ValidationReport validationReport = new ValidationReport(codespace, String.valueOf(System.currentTimeMillis()));
                    exchange.getIn().setBody(validationReport);
                })
                .setHeader(VALIDATION_REPORT_ID, simple("${body.validationReportId}"))
                .to("direct:validateSchema")
                // do not run subsequent validators if the schema validation failed
                .filter(PredicateBuilder.not(simple("${body.hasError()}")))
                .to("direct:validateAuthorityId")
                .end()
                .choice()
                .when(simple("${body.hasError()}"))
                .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_FAILED))
                .otherwise()
                .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_OK))
                .end()
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
                .setHeader(FILE_HANDLE, header(DATASET_CODESPACE).append(Constants.VALIDATION_REPORT_PREFIX).append(header(VALIDATION_REPORT_ID)).append(".json"))
                .log(LoggingLevel.INFO, correlation() + "Uploading Validation Report  to GCS file ${header." + FILE_HANDLE + "}")
                .to("direct:uploadAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Uploaded Validation Report to GCS file ${header." + FILE_HANDLE + "}")
                .routeId("upload-validation-report");

        from("direct:notifyMarduk")
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationStatusQueue")
                .routeId("notify-marduk");


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


    }
}
