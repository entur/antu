package no.entur.antu.validator.schema;

import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.xml.NetexSchemaRepository;
import org.rutebanken.netex.validation.NeTExValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Validate that NeTEX files are valid according to the XML schema.
 */
public class NetexSchemaValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexSchemaValidator.class);

    private final NetexSchemaRepository netexSchemaRepository;
    private final int maxValidationReportEntries;


    public NetexSchemaValidator(int maxValidationReportEntries) {
        this.netexSchemaRepository = new NetexSchemaRepository();
        this.maxValidationReportEntries = maxValidationReportEntries;
    }

    public List<ValidationReportEntry> validateSchema(String fileName, byte[] content) {

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        try {
            NeTExValidator.NetexVersion schemaVersion = NetexSchemaRepository.detectNetexSchemaVersion(content);
            if (schemaVersion == null) {
                schemaVersion = NeTExValidator.LATEST;
                LOGGER.warn("Could not detect schema version for file {}, defaulting to latest ({}})", fileName, schemaVersion);
            }
            Validator validator = netexSchemaRepository.getNetexSchema(schemaVersion).newValidator();
            validator.setErrorHandler(new ErrorHandler() {

                private int errorCount;

                @Override
                public void warning(SAXParseException exception) throws SAXParseException {
                    addValidationReportEntry(fileName, exception, ValidationReportEntrySeverity.WARNING);
                    errorCount++;
                }

                @Override
                public void error(SAXParseException exception) throws SAXParseException {
                    addValidationReportEntry(fileName, exception, ValidationReportEntrySeverity.ERROR);
                    errorCount++;
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXParseException {
                    error(exception);
                }

                private void addValidationReportEntry(String fileName, SAXParseException saxParseException, ValidationReportEntrySeverity severity) throws SAXParseException {
                    if (errorCount < maxValidationReportEntries) {
                        String message = "Line " + saxParseException.getLineNumber() + ", Column " + saxParseException.getColumnNumber() + ": " + saxParseException.getMessage();
                        validationReportEntries.add(new ValidationReportEntry(message, "NETEX_SCHEMA", severity, fileName));
                    } else {
                        LOGGER.warn("File {} has too many schema validation errors (max is {}). Additional errors will not be reported.", fileName, maxValidationReportEntries);
                        throw saxParseException;
                    }

                }

            });

            validator.validate(new StreamSource(new ByteArrayInputStream(content)));
        } catch (IOException e) {
            throw new AntuException(e);
        } catch (SAXException saxException) {
            LOGGER.info("Found schema validation errors");
        }

        return validationReportEntries;
    }


}
