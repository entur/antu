package no.entur.antu.commondata.scraper;

import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.validation.validator.xpath.ValidationContext;

public abstract class CommonDataScraper {

  // TODO: NEXT
  // Use the ServiceJourneyId record instead of strings.
  //  create scraper for common data caching from common file.
  //  Create scraper for Active dates for service journeys, which will be used for the validation shareed active date for interchange.

  public final void scrapeData(ValidationContext validationContext) {
    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      if (validationContext.isCommonFile()) {
        scrapeDataFromCommonFile(validationContextWithNetexEntitiesIndex);
      } else {
        scrapeDataFromLineFile(validationContextWithNetexEntitiesIndex);
      }
    } else {
      throw new IllegalArgumentException(
        "ValidationContext must be of type ValidationContextWithNetexEntitiesIndex"
      );
    }
  }

  protected abstract void scrapeDataFromLineFile(
    ValidationContextWithNetexEntitiesIndex validationContext
  );

  protected abstract void scrapeDataFromCommonFile(
    ValidationContextWithNetexEntitiesIndex validationContext
  );
}
