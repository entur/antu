package no.entur.antu.validation.validator.servicejourney.servicealteration.support;

import java.util.Collection;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.rutebanken.netex.model.DatedServiceJourney;

public class ServiceAlterationUtils {

  private ServiceAlterationUtils() {}

  public static Collection<DatedServiceJourney> datedServiceJourneysWithReferenceToOriginal(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .datedServiceJourneys()
      .stream()
      .filter(dsj ->
        dsj.getReplacedJourneys() != null &&
        !dsj
          .getReplacedJourneys()
          .getDatedVehicleJourneyRefOrNormalDatedVehicleJourneyRef()
          .isEmpty()
      )
      .toList();
  }
}
