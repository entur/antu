package no.entur.antu.validation.flex.validator;

import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;
import org.entur.netex.validation.validator.xpath.tree.ValidationTreeBuilder;

/**
 * XPath's validation tree for flexible transport timetable data, imported through OperatørPortalen.
 */
public class EnturImportFlexTimetableDataValidationTreeFactory
  extends EnturFlexTimetableDataValidationTreeFactory {

  public static final String CODE_LINE_10 = "LINE_10";

  public EnturImportFlexTimetableDataValidationTreeFactory(
    OrganisationAliasRepository organisationAliasRepository
  ) {
    super(organisationAliasRepository);
  }

  @Override
  public ValidationTreeBuilder builder() {
    // reject non-flex lines in imported flex dataset
    serviceFrameValidationTreeBuilder()
      .withRuleForLineFile(
        new ValidateNotExist(
          "lines/Line",
          CODE_LINE_10,
          "Flexible line - Line not allowed",
          "Line not allowed in imported flexible line files",
          Severity.ERROR
        )
      );
    return super.builder();
  }
}
