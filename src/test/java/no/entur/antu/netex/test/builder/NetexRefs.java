package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import jakarta.xml.bind.JAXBElement;
import java.util.Collections;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.OperatorRefStructure;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceLinkRefStructure;
import org.rutebanken.netex.model.StopPlaceRefStructure;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

/**
 * Ref structures for entities the builders do not create themselves, under the same
 * {@code TST:<EntityType>:<id>} convention the builders use.
 */
public final class NetexRefs {

  private static final DayType EVERYDAY = new DayType()
    .withId("EVERYDAY")
    .withName(new MultilingualString().withValue("everyday"));

  private NetexRefs() {}

  public static ScheduledStopPointRefStructure scheduledStopPointRef(int id) {
    return new ScheduledStopPointRefStructure()
      .withRef("TST:ScheduledStopPoint:" + id);
  }

  public static OperatorRefStructure operatorRef(int id) {
    return new OperatorRefStructure().withRef("TST:Operator:" + id);
  }

  public static QuayRefStructure quayRef(int id) {
    return new QuayRefStructure().withRef("TST:Quay:" + id);
  }

  /**
   * A StopPlaceRefStructure carrying a {@code TST:StopPoint:<id>} ref. The mismatch between the
   * structure type and the id is inherited from the original test data factory; several tests
   * assert on the literal.
   */
  public static StopPlaceRefStructure stopPointRef(int id) {
    return new StopPlaceRefStructure().withRef("TST:StopPoint:" + id);
  }

  public static ServiceLinkRefStructure serviceLinkRef(int id) {
    return new ServiceLinkRefStructure().withRef("TST:ServiceLink:" + id);
  }

  public static VehicleJourneyRefStructure serviceJourneyRef(int id) {
    return new VehicleJourneyRefStructure().withRef("TST:ServiceJourney:" + id);
  }

  public static DestinationDisplayRefStructure destinationDisplayRef(int id) {
    return new DestinationDisplayRefStructure()
      .withRef("TST:DestinationDisplay:" + id);
  }

  /**
   * The single day type every journey the builders produce runs on.
   */
  static DayTypeRefs_RelStructure everyDayRefs() {
    return new DayTypeRefs_RelStructure()
      .withDayTypeRef(Collections.singleton(everyDayRef()));
  }

  private static JAXBElement<DayTypeRefStructure> everyDayRef() {
    return createJaxbElement(
      new DayTypeRefStructure().withRef(EVERYDAY.getId())
    );
  }
}
