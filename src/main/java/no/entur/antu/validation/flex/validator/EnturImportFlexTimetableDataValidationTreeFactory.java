package no.entur.antu.validation.flex.validator;

import no.entur.antu.organisation.OrganisationRepository;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * XPath's validation tree for flexible transport timetable data, imported through OperatørPortalen.
 */
public class EnturImportFlexTimetableDataValidationTreeFactory
  extends EnturFlexTimetableDataValidationTreeFactory {

  public EnturImportFlexTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    super(organisationRepository);
  }

  @Override
  protected ValidationTree getServiceFrameValidationTreeForLineFile(
    String path
  ) {
    ValidationTree serviceFrameValidationTreeForLineFile =
      super.getServiceFrameValidationTreeForLineFile(path);
    serviceFrameValidationTreeForLineFile.addValidationRule(
      new ValidateNotExist(
        "lines/Line",
        "LINE_10",
        "Flexible line - Line not allowed",
        "Line not allowed in imported flexible line files",
        Severity.ERROR
      )
    );
    return serviceFrameValidationTreeForLineFile;
  }
}
