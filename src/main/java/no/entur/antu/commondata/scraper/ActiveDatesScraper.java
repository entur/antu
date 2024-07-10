package no.entur.antu.commondata.scraper;

import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;

public class ActiveDatesScraper extends CommonDataScraper {

  @Override
  protected void scrapeDataFromLineFile(
    ValidationContextWithNetexEntitiesIndex validationContext
  ) {
    if (validationContext.getAntuNetexData().hasServiceCalenderFrames()) {
      ServiceCalendarFrameParser serviceCalendarFrameParser =
        new ServiceCalendarFrameParser();
      validationContext
        .getAntuNetexData()
        .serviceCalendarFrames()
        .forEach(serviceCalendarFrameParser::parse);
    }
  }

  @Override
  protected void scrapeDataFromCommonFile(
    ValidationContextWithNetexEntitiesIndex validationContext
  ) {}
}
