package no.entur.antu.validation.validator.id;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import no.entur.antu.validation.NetexCodespace;
import no.entur.antu.validation.utilities.CodespaceUtils;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.XPathValidator;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that NeTEX IDs have a valid structure.
 */
public class NetexIdValidator implements XPathValidator {

  static final ValidationRule RULE_INVALID_ID_STRUCTURE = new ValidationRule(
    "NETEX_ID_2",
    "NeTEx ID invalid structure",
    "Invalid id structure on element",
    Severity.ERROR
  );

  static final ValidationRule RULE_INVALID_ID_NAME = new ValidationRule(
    "NETEX_ID_3",
    "NeTEx ID invalid element name inside structure",
    "Invalid structure on id %s. Expected %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_UNAPPROVED_CODESPACE = new ValidationRule(
    "NETEX_ID_4",
    "NeTEx ID with unapproved codespace",
    "Use of unapproved codespace. Approved codespaces are %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_UNAPPROVED_CODESPACE_WARNING =
    new ValidationRule(
      "NETEX_ID_4W",
      "NeTEx ID with unapproved codespace on operator",
      "Use of unapproved codespace. Approved codespaces are %s",
      Severity.WARNING
    );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    NetexIdValidator.class
  );

  private static final String REGEXP_VALID_ID =
    "^([A-Z]{3}):([A-Za-z]*):([0-9A-Za-z_\\-]*)$";
  private static final Pattern PATTERN_VALID_ID = Pattern.compile(
    REGEXP_VALID_ID
  );

  private final Set<String> entityTypesReportedAsWarningForUnapprovedCodespace;
  private final Set<NetexCodespace> additionalAllowedCodespaces;

  public NetexIdValidator(Set<NetexCodespace> additionalAllowedCodespaces) {
    this(Set.of(), additionalAllowedCodespaces);
  }

  public NetexIdValidator(
    Set<String> entityTypesReportedAsWarningForUnapprovedCodespace,
    Set<NetexCodespace> additionalAllowedCodespaces
  ) {
    this.entityTypesReportedAsWarningForUnapprovedCodespace =
      Objects.requireNonNull(
        entityTypesReportedAsWarningForUnapprovedCodespace
      );
    this.additionalAllowedCodespaces = additionalAllowedCodespaces;
  }

  private List<NetexCodespace> getValidCodespacesFor(String codespace) {
    List<NetexCodespace> validCodespaces = new ArrayList<>();
    if (additionalAllowedCodespaces != null) {
      validCodespaces.addAll(additionalAllowedCodespaces);
    }
    validCodespaces.add(NetexCodespace.rutebanken(codespace));
    return validCodespaces;
  }

  @Override
  public List<ValidationIssue> validate(
    XPathValidationContext validationContext
  ) {
    List<ValidationIssue> validationIssues = new ArrayList<>();
    String codespace = validationContext.getCodespace();
    Set<String> validNetexCodespaces = CodespaceUtils
      .getValidCodespacesFor(codespace, additionalAllowedCodespaces)
      .stream()
      .map(NetexCodespace::xmlns)
      .collect(Collectors.toSet());
    String validNetexCodespaceList = String.join(",", validNetexCodespaces);

    for (IdVersion id : validationContext.getLocalIds()) {
      Matcher m = PATTERN_VALID_ID.matcher(id.getId());
      DataLocation dataLocation = id.dataLocation();
      if (!m.matches()) {
        validationIssues.add(
          new ValidationIssue(RULE_INVALID_ID_STRUCTURE, dataLocation)
        );
        LOGGER.debug(
          "Id {} has an invalid format. Valid format is {}",
          id,
          REGEXP_VALID_ID
        );
      } else {
        if (!m.group(2).equals(id.getElementName())) {
          String expectedId =
            m.group(1) + ":" + id.getElementName() + ":" + m.group(3);
          validationIssues.add(
            new ValidationIssue(
              RULE_INVALID_ID_NAME,
              dataLocation,
              id.getId(),
              expectedId
            )
          );
          LOGGER.debug(
            "Id {} has an invalid format for the name part. Expected {}",
            id,
            expectedId
          );
        }

        String prefix = m.group(1);
        if (!validNetexCodespaces.contains(prefix)) {
          LOGGER.debug(
            "Id {} uses an unapproved codespace prefix. Approved codespaces are: {}",
            id,
            validNetexCodespaceList
          );
          if (
            entityTypesReportedAsWarningForUnapprovedCodespace.contains(
              id.getElementName()
            )
          ) {
            validationIssues.add(
              new ValidationIssue(
                RULE_UNAPPROVED_CODESPACE_WARNING,
                dataLocation,
                validNetexCodespaceList
              )
            );
          } else {
            validationIssues.add(
              new ValidationIssue(
                RULE_UNAPPROVED_CODESPACE,
                dataLocation,
                validNetexCodespaceList
              )
            );
          }
        }
      }
    }
    return validationIssues;
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(
      RULE_INVALID_ID_STRUCTURE,
      RULE_INVALID_ID_NAME,
      RULE_UNAPPROVED_CODESPACE,
      RULE_UNAPPROVED_CODESPACE_WARNING
    );
  }
}
