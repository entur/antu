package no.entur.antu.validator.schema;

import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Validate that NeTEX files are valid according to the XML schema.
 */
public class NetexSchemaValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexSchemaValidator.class);
    private final int maxValidationReportEntries;

    public NetexSchemaValidator(int maxValidationReportEntries) {
        this.maxValidationReportEntries = maxValidationReportEntries;
    }


    public List<ValidationReportEntry> validateSchema(InputStream timetableDataset) {

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(timetableDataset)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String zipEntryName = zipEntry.getName();
                if (zipEntryName.endsWith(".xml")) {
                    LOGGER.info("Validating NeTEx file {}", zipEntryName);
                    byte[] allBytes = zipInputStream.readAllBytes();
                    validationReportEntries.addAll(validateNetexFile(zipEntryName, allBytes));

                } else {
                    LOGGER.info("Ignoring non-xml file {}", zipEntryName);
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            throw new AntuException("Error while loading the NeTEx archive", e);
        }

        return validationReportEntries;
    }

    private List<ValidationReportEntry> validateNetexFile(String fileName, byte[] allBytes) {

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        NeTExValidator neTExValidator;
        try {
            neTExValidator = NeTExValidator.getNeTExValidator();
            Validator validator = neTExValidator.getSchema().newValidator();
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
                    if(errorCount < maxValidationReportEntries) {
                        String message = "Line " + saxParseException.getLineNumber() + ", Column " + saxParseException.getColumnNumber() + ": " + saxParseException.getMessage();
                        validationReportEntries.add(new ValidationReportEntry(message, "NeTEx Schema Validation", severity, fileName));
                    } else {
                        LOGGER.warn("File {} has too many schema validation errors (max is {}). Additional errors will not be reported.", fileName, maxValidationReportEntries);
                        throw saxParseException;
                    }

                }

            });

            validator.validate(new StreamSource(new ByteArrayInputStream(allBytes)));
        } catch (IOException e) {
            throw new AntuException(e);
        } catch (SAXException saxException) {
            LOGGER.info("Found schema validation errors");
        }

        return validationReportEntries;
    }


}
