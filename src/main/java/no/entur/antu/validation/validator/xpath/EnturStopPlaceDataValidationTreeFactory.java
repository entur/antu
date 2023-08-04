package no.entur.antu.validation.validator.xpath;

import org.entur.netex.validation.validator.xpath.ValidationTree;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;

public class EnturStopPlaceDataValidationTreeFactory
  implements ValidationTreeFactory {

  @Override
  public ValidationTree buildValidationTree() {
    return new ValidationTree("PublicationDelivery", "/");
  }
}
