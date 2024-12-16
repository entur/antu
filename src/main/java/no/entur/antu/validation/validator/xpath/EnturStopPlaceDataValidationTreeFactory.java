package no.entur.antu.validation.validator.xpath;

import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.tree.ValidationTreeBuilder;

public class EnturStopPlaceDataValidationTreeFactory
  implements ValidationTreeFactory {

  @Override
  public ValidationTreeBuilder builder() {
    return ValidationTreeBuilder.empty();
  }
}
