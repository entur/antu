package no.entur.antu.commondata;

import org.entur.netex.validation.validator.xpath.ValidationContext;

public interface CommonDataScraper {
  void scrapeData(ValidationContext validationContext);
}
