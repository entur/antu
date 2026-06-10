package no.entur.antu.netexdata.collectors;

import no.entur.antu.memorystore.LineInfoMemStoreRepository;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.SimpleLine;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;

public class LineInfoCollector extends NetexDataCollector {

  private final LineInfoMemStoreRepository lineInfoMemStoreRepository;

  public LineInfoCollector(
    LineInfoMemStoreRepository lineInfoMemStoreRepository
  ) {
    this.lineInfoMemStoreRepository = lineInfoMemStoreRepository;
  }

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext validationContext
  ) {
    SimpleLine lineInfo = lineInfo(validationContext);
    if (lineInfo != null) {
      lineInfoMemStoreRepository.addLineInfo(
        validationContext.getValidationReportId(),
        lineInfo.toString()
      );
    }
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    // No Lines in common files
  }

  private SimpleLine lineInfo(JAXBValidationContext validationContext) {
    return validationContext
      .lines()
      .stream()
      .findFirst()
      .map(line -> SimpleLine.of(line, validationContext.getFileName()))
      .orElse(
        validationContext
          .flexibleLines()
          .stream()
          .filter(f ->
            f.getFlexibleLineType() == FlexibleLineTypeEnumeration.FIXED
          )
          .findFirst()
          .map(line -> SimpleLine.of(line, validationContext.getFileName()))
          .orElse(null)
      );
  }
}
