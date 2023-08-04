package no.entur.antu.validator.journeypattern.stoppoint.distance;

import static no.entur.antu.validator.journeypattern.stoppoint.distance.UnexpectedDistanceContextBuilder.*;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.utilities.AntuNetexData;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the distance between stops in journey patterns is as expected.
 */
public class UnexpectedDistance extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedDistance.class
  );
  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  protected UnexpectedDistance(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(validationReportEntryFactory);
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return new RuleCode[0];
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating distance between stops in journey patterns");

    AntuNetexData antuNetexData = createAntuNetexData(validationContext);

    UnexpectedDistanceContextBuilder builder =
      new UnexpectedDistanceContextBuilder(
        antuNetexData.withStopPlacesAndCommonData(
          commonDataRepository,
          stopPlaceRepository
        )
      );
    /*
    antuNetexData.entitiesIndex()
      .getJourneyPatternIndex()
      .getAll()
      .stream()
      .map(builder::build)
*/
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    // JourneyPatterns only appear in the Line file.
  }

  private void validateDistance(UnexpectedDistanceContext distanceContext) {}
}
