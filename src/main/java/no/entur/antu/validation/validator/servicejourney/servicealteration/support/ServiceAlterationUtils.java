package no.entur.antu.validation.validator.servicejourney.servicealteration.support;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DatedServiceJourneyRefStructure;

public class ServiceAlterationUtils {

  private ServiceAlterationUtils() {}

  public static Collection<DatedServiceJourney> datedServiceJourneysWithReferenceToOriginal(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .datedServiceJourneys()
      .stream()
      .filter(dsj ->
        dsj
          .getJourneyRef()
          .stream()
          .map(JAXBElement::getValue)
          .anyMatch(DatedServiceJourneyRefStructure.class::isInstance)
      )
      .toList();
  }
}
