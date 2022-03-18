package no.entur.antu.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.entur.netex.validation.validator.ValidationReport;

import java.io.IOException;
import java.io.InputStream;

public final class TestValidationReportUtil {

    private TestValidationReportUtil() {
    }

    public static ValidationReport getValidationReport(InputStream reportInputStream) throws IOException {
        assert reportInputStream != null;
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper.readerFor(ValidationReport.class).readValue(reportInputStream);
    }
}
