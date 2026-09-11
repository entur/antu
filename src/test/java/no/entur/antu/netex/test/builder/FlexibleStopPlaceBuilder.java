package no.entur.antu.netex.test.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.FlexibleStopPlace_VersionStructure;
import org.rutebanken.netex.model.MultilingualString;

public class FlexibleStopPlaceBuilder extends EntityBuilder<FlexibleStopPlace> {

  private FlexibleAreaBuilder flexibleArea;
  private final List<String> scheduledStopPointRefs = new ArrayList<>();

  public FlexibleStopPlaceBuilder(int id) {
    super("FlexibleStopPlace", id);
  }

  /**
   * Assign this flexible stop place to scheduled stop points, as a line file's own
   * FlexibleStopPlace assignment would. Without this the flexible stop place is only reachable by
   * its own id, and JAXBValidationContext.flexibleStopPlaceRefFromScheduledStopPointRef cannot
   * resolve it. For the shared-file equivalent, use
   * TestCommonDataRepository.putFlexibleStopPlaceRef.
   *
   * @param scheduledStopPointRefs the scheduled stop point ids this flexible stop place serves
   * @return FlexibleStopPlaceBuilder
   */
  public FlexibleStopPlaceBuilder withScheduledStopPointRefs(
    String... scheduledStopPointRefs
  ) {
    Collections.addAll(this.scheduledStopPointRefs, scheduledStopPointRefs);
    return this;
  }

  public List<String> scheduledStopPointRefs() {
    return scheduledStopPointRefs;
  }

  /**
   * Adds a flexible area to this flexible stop place, if it does not already have one.
   *
   * @param id the id of the flexible area
   * @return FlexibleAreaBuilder
   */
  public FlexibleAreaBuilder addFlexibleArea(int id) {
    if (flexibleArea == null) {
      flexibleArea = new FlexibleAreaBuilder(id);
    }
    return flexibleArea;
  }

  @Override
  public FlexibleStopPlace build() {
    return new FlexibleStopPlace()
      .withId(ref())
      .withName(new MultilingualString().withValue("FlexibleStopPlace " + id))
      .withAreas(
        new FlexibleStopPlace_VersionStructure.Areas()
          .withFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea(
            Optional
              .ofNullable(flexibleArea)
              .map(FlexibleAreaBuilder::build)
              .orElse(null)
          )
      );
  }
}
