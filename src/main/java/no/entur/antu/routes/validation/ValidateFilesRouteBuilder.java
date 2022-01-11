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


import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.validator.ValidationReport;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.validator.id.IdVersion;
import no.entur.antu.validator.id.NetexIdExtractorHelper;
import no.entur.antu.validator.xpath.ValidationContext;
import no.entur.antu.xml.XMLParserUtil;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_WORK;
import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;


/**
 * Validate NeTEx files, both common files and line files.
 *
 */
@Component
public class ValidateFilesRouteBuilder extends BaseRouteBuilder {

    private static final String PROP_NETEX_FILE_CONTENT = "NETEX_FILE_CONTENT";
    private static final String PROP_VALIDATION_REPORT = "VALIDATION_REPORT";
    public static final String PROP_VALIDATION_CONTEXT = "VALIDATION_CONTEXT";
    public static final String PROP_LOCAL_IDS = "LOCAL_IDS";
    public static final String PROP_LOCAL_REFS = "LOCAL_REFS";
    private static final String PROP_ALL_NETEX_FILE_NAMES ="ALL_NETEX_FILE_NAMES";

    @Override
    public void configure() throws Exception {
        super.configure();

        from("direct:validateNetex")
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx file ${header." + FILE_HANDLE + "}")
                .setProperty(PROP_ALL_NETEX_FILE_NAMES, body())
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

        from("direct:downloadSingleNetexFile").streamCaching()
                .log(LoggingLevel.INFO, correlation() + "Downloading single NeTEx file ${header." + FILE_HANDLE + "}")
                .to("direct:getAntuBlob")
                .log(LoggingLevel.INFO, correlation() + "Downloaded single NeTEx file ${header." + FILE_HANDLE + "}")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx file not found: ${header." + FILE_HANDLE + "}")
                .stop()
                //end filter
                .end()
                .unmarshal().zipFile()
                .routeId("download-single-netex-file");

        from("direct:runNetexValidators").streamCaching()
                .log(LoggingLevel.INFO, correlation() + "Running NeTEx validators")
                .filter(simple("${properties:antu.schema.validation.enabled:true}"))
                .to("direct:validateSchema")
                // end filter
                .end()
                // do not run subsequent validators if the schema validation failed
                .filter(PredicateBuilder.not(simple("${exchangeProperty." + PROP_VALIDATION_REPORT + ".hasError()}")))
                .process(exchange -> {
                    byte[] content = exchange.getProperty(PROP_NETEX_FILE_CONTENT, byte[].class);
                    String codespace = exchange.getIn().getHeader(DATASET_CODESPACE, String.class);
                    String fileName = exchange.getIn().getHeader(NETEX_FILE_NAME, String.class);
                    XdmNode document = XMLParserUtil.parseFileToXdmNode(content);
                    ValidationContext validationContext = new ValidationContext(document, XMLParserUtil.getXPathCompiler(), codespace, fileName);
                    exchange.setProperty(PROP_VALIDATION_CONTEXT, validationContext);
                })
                .to("direct:validateXPath")
                .process(exchange -> {
                    ValidationContext validationContext = exchange.getProperty(PROP_VALIDATION_CONTEXT, ValidationContext.class);
                    List<IdVersion> localIdList = NetexIdExtractorHelper.collectEntityIdentificators(validationContext, Set.of("Codespace"));
                    Set<IdVersion> localIds = new HashSet<>(localIdList);
                    exchange.setProperty(PROP_LOCAL_IDS, localIds);
                })
                .filter(header(NETEX_FILE_NAME).startsWith("_"))
                .bean("commonNetexIdRepository", "addCommonNetexIds(${header." + VALIDATION_REPORT_ID + "},${exchangeProperty." + PROP_LOCAL_IDS + "})")
                //end filter
                .end()
                .to("direct:validateIds")
                .to("direct:validateVersionOnLocalIds")
                .process(exchange -> {
                    ValidationContext validationContext = exchange.getProperty(PROP_VALIDATION_CONTEXT, ValidationContext.class);
                    List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(validationContext, null);
                    exchange.setProperty(PROP_LOCAL_REFS, localRefs);
                })
                .to("direct:validateVersionOnRefToLocalIds")
                .to("direct:validateReferenceToValidEntityType")
                .to("direct:validateExternalReference")
                .to("direct:validateDuplicatedNetexIds")
                // end filter
                .end()
                .log(LoggingLevel.INFO, correlation() + "Completed all NeTEx validators")
                .routeId("run-netex-validators");

        from("direct:validateSchema").streamCaching()
                .log(LoggingLevel.INFO, correlation() + "Running NeTEx schema validation")
                .setBody(method("netexSchemaValidator", "validateSchema(${header." + NETEX_FILE_NAME + "},${exchangeProperty." + PROP_NETEX_FILE_CONTENT + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "NeTEx schema validation complete")
                .routeId("validate-schema");

        from("direct:validateXPath").streamCaching()
                .log(LoggingLevel.INFO, correlation() + "Running XPath validation")
                .setBody(method("xpathValidator", "validate(${exchangeProperty." + PROP_VALIDATION_CONTEXT + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "XPath validation complete")
                .routeId("validate-xpath");

        from("direct:validateIds")
                .log(LoggingLevel.INFO, correlation() + "Running IDs validation")
                .setBody(method("netexIdValidator", "validate(${exchangeProperty." + PROP_VALIDATION_CONTEXT + "}, ${exchangeProperty." + PROP_LOCAL_IDS + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "IDs validation complete")
                .routeId("validate-ids");

        from("direct:validateVersionOnLocalIds")
                .log(LoggingLevel.INFO, correlation() + "Running validation of version on local IDs")
                .setBody(method("versionOnLocalNetexIdValidator", "validate(${exchangeProperty." + PROP_LOCAL_IDS + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "validation of version on local IDs complete")
                .routeId("validate-version-on-local-ids");

        from("direct:validateVersionOnRefToLocalIds")
                .log(LoggingLevel.INFO, correlation() + "Running validation of version on ref to local IDs")
                .setBody(method("versionOnRefToLocalNetexIdValidator", "validate(${exchangeProperty." + PROP_LOCAL_IDS + "}, ${exchangeProperty." + PROP_LOCAL_REFS + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "Validation of version on ref to local IDs complete")
                .routeId("validate-version-on-ref-to-local-ids");

        from("direct:validateReferenceToValidEntityType")
                .log(LoggingLevel.INFO, correlation() + "Running validation of reference entity type")
                .setBody(method("refToValidEntityTypeValidator", "validate(${exchangeProperty." + PROP_LOCAL_REFS + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "Validation of reference entity type complete")
                .routeId("validate-ref-entity-type");

        from("direct:validateExternalReference")
                .log(LoggingLevel.INFO, correlation() + "Running validation of external reference")
                .setBody(method("neTexReferenceValidator", "validate(${header." + VALIDATION_REPORT_ID + "}, ${exchangeProperty." + PROP_LOCAL_REFS + "}, ${exchangeProperty." + PROP_LOCAL_IDS + "})"))
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "Validation of external reference complete")
                .routeId("validate-external-ref");

        from("direct:validateDuplicatedNetexIds")
                .log(LoggingLevel.INFO, correlation() + "Running validation of duplicated NeTEx Ids")
                .bean("netexIdUniquenessValidator", "validate(${header." + VALIDATION_REPORT_ID + "}, ${header." + NETEX_FILE_NAME + "}, ${exchangeProperty." + PROP_LOCAL_IDS + "})")
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    validationReport.addAllValidationReportEntries(exchange.getIn().getBody(Collection.class));
                })
                .log(LoggingLevel.INFO, correlation() + "validation of duplicated NeTEx Ids complete")
                .routeId("validate-duplicated-netex-ids");


        from("direct:reportSystemError")
                .process(exchange -> {
                    ValidationReport validationReport = exchange.getProperty(PROP_VALIDATION_REPORT, ValidationReport.class);
                    ValidationReportEntry validationReportEntry = new ValidationReportEntry("System error while validating the  file " + exchange.getIn().getHeader(NETEX_FILE_NAME), "SYSTEM_ERROR", ValidationReportEntrySeverity.ERROR, exchange.getIn().getHeader(NETEX_FILE_NAME, String.class));
                    validationReport.addValidationReportEntry(validationReportEntry);
                })
                .routeId("report-system-error");

        from("direct:saveValidationReport")
                .log(LoggingLevel.INFO, correlation() + "Saving validation report")
                .setBody(exchangeProperty(PROP_VALIDATION_REPORT))
                .marshal().json(JsonLibrary.Jackson)
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
                .filter(header(NETEX_FILE_NAME).startsWith("_"))
                .log(LoggingLevel.INFO, correlation() + "Notifying common files aggregator")
                .setBody(exchangeProperty(PROP_ALL_NETEX_FILE_NAMES))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuCommonFilesAggregationQueue")
                //end filter
                .end()
                .routeId("notify-validation-report-aggregator");

    }
}
