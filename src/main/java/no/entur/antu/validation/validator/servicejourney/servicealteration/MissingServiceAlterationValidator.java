package no.entur.antu.validation.validator.servicejourney.servicealteration;

import java.util.stream.Stream;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.ServiceJourney;

public class MissingServiceAlterationValidator extends AntuNetexValidator {

  protected MissingServiceAlterationValidator(ValidationReportEntryFactory validationReportEntryFactory) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return MissingServiceAlterationError.RuleCode.values();
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    Stream<ServiceJourney> serviceJourneyStream =
      antuNetexData.validServiceJourneys();

    antuNetexData
  }
}
