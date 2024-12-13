package no.entur.antu.validation.flex.validator;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.XPathValidator;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;

public class FileNameValidator implements XPathValidator {

  static final ValidationRule RULE = new ValidationRule(
    "NETEX_FILE_NAME_1",
    "Invalid file name",
    "Invalid filename: %s",
    Severity.ERROR
  );

  private static final Pattern SHARED_FILE_PATTERN = Pattern.compile(
    "_(\\w{3})(_flexible)?_shared_data.xml"
  );
  private static final Pattern LINE_FILE_PATTERN = Pattern.compile(
    "(\\w{3})_.*\\.xml"
  );

  @Override
  public List<ValidationIssue> validate(
    XPathValidationContext validationContext
  ) {
    String fileName = validationContext.getFileName();

    // If it's not an XML file, we will simply ignore it.
    if (FilenameUtils.getExtension(fileName).equals("xml")) {
      boolean isSharedFile = SHARED_FILE_PATTERN.matcher(fileName).matches();
      boolean isLineFile = LINE_FILE_PATTERN.matcher(fileName).matches();

      if (!isSharedFile && !isLineFile) {
        return List.of(
          new ValidationIssue(
            RULE,
            new DataLocation(null, fileName, 0, 0),
            fileName
          )
        );
      }
    }
    return List.of();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }
}
