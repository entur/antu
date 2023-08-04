package no.entur.antu.validation.flex.validator;

import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.entur.netex.validation.validator.AbstractNetexValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileNameValidator extends AbstractNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    FileNameValidator.class
  );
  protected static final String RULE_CODE_NETEX_FILE_NAME_1 =
    "NETEX_FILE_NAME_1";
  private static final Pattern SHARED_FILE_PATTERN = Pattern.compile(
    "_(\\w{3})(_flexible)?_shared_data.xml"
  );
  private static final Pattern LINE_FILE_PATTERN = Pattern.compile(
    "(\\w{3})_.*\\.xml"
  );

  public FileNameValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    String fileName = validationContext.getFileName();

    // If it's not an XML file, we will simply ignore it.
    if (FilenameUtils.getExtension(fileName).equals("xml")) {
      boolean isSharedFile = SHARED_FILE_PATTERN.matcher(fileName).matches();
      boolean isLineFile = LINE_FILE_PATTERN.matcher(fileName).matches();

      if (!isSharedFile && !isLineFile) {
        validationReport.addValidationReportEntry(
          createValidationReportEntry(
            RULE_CODE_NETEX_FILE_NAME_1,
            new DataLocation(null, fileName, 0, 0),
            String.format("Invalid filename: %s", fileName)
          )
        );
        LOGGER.debug("Filename has invalid pattern");
      }
    }
  }

  @Override
  public Set<String> getRuleDescriptions() {
    return Set.of(
      createRuleDescription(RULE_CODE_NETEX_FILE_NAME_1, "Invalid filename")
    );
  }
}
