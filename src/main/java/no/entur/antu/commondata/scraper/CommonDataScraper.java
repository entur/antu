package no.entur.antu.commondata.scraper;

import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.validation.validator.xpath.ValidationContext;

public abstract class CommonDataScraper {

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
