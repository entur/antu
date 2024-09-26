package no.entur.antu.commondata;

import org.entur.netex.validation.validator.xpath.XPathValidationContext;

public interface CommonDataScraper {
  void scrapeData(XPathValidationContext validationContext);
}
