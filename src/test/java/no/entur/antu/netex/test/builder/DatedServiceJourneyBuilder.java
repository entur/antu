package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.Collection;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DatedServiceJourneyRefStructure;
import org.rutebanken.netex.model.JourneyRefStructure;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourneyRefStructure;

public class DatedServiceJourneyBuilder
  extends EntityBuilder<DatedServiceJourney> {

  private final OperatingDayBuilder operatingDayRef;
  private final ServiceJourneyBuilder serviceJourneyRef;
  private DatedServiceJourneyBuilder datedServiceJourneyRef;
  private ServiceAlterationEnumeration serviceAlteration;

  public DatedServiceJourneyBuilder(
    int id,
    ServiceJourneyBuilder serviceJourneyRef,
    OperatingDayBuilder operatingDayRef
  ) {
    super("DatedServiceJourney", id);
    this.serviceJourneyRef = serviceJourneyRef;
    this.operatingDayRef = operatingDayRef;
  }

  public DatedServiceJourneyBuilder withServiceAlteration(
    ServiceAlterationEnumeration serviceAlteration
  ) {
    this.serviceAlteration = serviceAlteration;
    return this;
  }

  public DatedServiceJourneyBuilder withDatedServiceJourneyRef(
    DatedServiceJourneyBuilder datedServiceJourneyRef
  ) {
    this.datedServiceJourneyRef = datedServiceJourneyRef;
    return this;
  }

  @Override
  public DatedServiceJourney build() {
    DatedServiceJourney datedServiceJourney = new DatedServiceJourney()
      .withId(ref());

    Collection<JAXBElement<? extends JourneyRefStructure>> journeyRefs =
      new ArrayList<>();
    journeyRefs.add(
      createJaxbElement(
        new ServiceJourneyRefStructure().withRef(serviceJourneyRef.ref())
      )
    );
    if (datedServiceJourneyRef != null) {
      journeyRefs.add(
        createJaxbElement(
          new DatedServiceJourneyRefStructure()
            .withRef(datedServiceJourneyRef.ref())
        )
      );
    }

    return datedServiceJourney
      .withJourneyRef(journeyRefs)
      .withOperatingDayRef(
        new OperatingDayRefStructure().withRef(operatingDayRef.ref())
      )
      .withServiceAlteration(serviceAlteration);
  }
}
