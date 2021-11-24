package no.entur.antu.validator.schema;

import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
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
            e.printStackTrace();
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
                @Override
                public void warning(SAXParseException exception) {
                    validationReportEntries.add(createValidationReportEntry(fileName, exception, "WARNING"));
                }

                @Override
                public void error(SAXParseException exception) {
                    validationReportEntries.add(createValidationReportEntry(fileName, exception, "ERROR"));
                }

                @Override
                public void fatalError(SAXParseException exception) {
                    validationReportEntries.add(createValidationReportEntry(fileName, exception, "ERROR"));
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

    private ValidationReportEntry createValidationReportEntry(String fileName, SAXParseException saxParseException, String severity) {
        String message = saxParseException.getLineNumber() + ":" + saxParseException.getColumnNumber() + " " + saxParseException.getMessage();
        return new ValidationReportEntry(message, "NeTEx Schema Validation", severity, fileName);
    }


}
