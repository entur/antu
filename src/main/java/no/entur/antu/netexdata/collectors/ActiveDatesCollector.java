package no.entur.antu.netexdata.collectors;

import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;

// TODO: NEXT
//  Use the ServiceJourneyId record instead of strings. >DONE<
//  create scraper for common data caching from common file.
//  Create scraper for Active dates for service journeys,
//  which will be used for the validation shared active date for interchange.

public class ActiveDatesCollector extends NetexDataCollector {

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext validationContext
  ) {
    AntuNetexData antuNetexData = new AntuNetexData(
      validationContext.getValidationReportId(),
      validationContext.getNetexEntitiesIndex(),
      validationContext.getNetexDataRepository(),
      validationContext.getStopPlaceRepository()
    );
    if (antuNetexData.hasServiceCalenderFrames()) {
      ServiceCalendarFrameParser serviceCalendarFrameParser =
        new ServiceCalendarFrameParser();
      antuNetexData
        .serviceCalendarFrames()
        .forEach(serviceCalendarFrameParser::parse);
    }
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {}
}
