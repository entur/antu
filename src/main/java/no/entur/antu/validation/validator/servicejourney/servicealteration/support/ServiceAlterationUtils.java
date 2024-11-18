package no.entur.antu.validation.validator.servicejourney.servicealteration.support;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.Optional;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DatedServiceJourneyRefStructure;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public class ServiceAlterationUtils {

  private ServiceAlterationUtils() {}

  public static DatedServiceJourneyRefStructure datedServiceJourneyRef(
    DatedServiceJourney datedServiceJourney
  ) {
    return datedServiceJourney
      .getJourneyRef()
      .stream()
      .map(JAXBElement::getValue)
      .filter(DatedServiceJourneyRefStructure.class::isInstance)
      .map(DatedServiceJourneyRefStructure.class::cast)
      .findFirst()
      .orElse(null);
  }

  public static Collection<DatedServiceJourney> datedServiceJourneysWithReferenceToReplaced(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .datedServiceJourneys()
      .stream()
      .filter(dsj -> dsj.getJourneyRef() != null)
      .filter(dsj ->
        dsj
          .getJourneyRef()
          .stream()
          .map(JAXBElement::getValue)
          .anyMatch(DatedServiceJourneyRefStructure.class::isInstance)
      )
      .toList();
  }

  public static Collection<DatedServiceJourney> replacedDatedServiceJourneys(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .datedServiceJourneys()
      .stream()
      .filter(dsj ->
        dsj.getServiceAlteration() == ServiceAlterationEnumeration.REPLACED
      )
      .toList();
  }

  public static DatedServiceJourney datedServiceJourney(
    DatedServiceJourneyRefStructure datedServiceJourneyRefStructure,
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return Optional
      .ofNullable(datedServiceJourneyRefStructure)
      .map(DatedServiceJourneyRefStructure::getRef)
      .map(ref -> netexEntitiesIndex.getDatedServiceJourneyIndex().get(ref))
      .orElse(null);
  }
}
