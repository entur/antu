package no.entur.antu.netex.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.junit.jupiter.api.Test;

class NetexTestDataFlexibleStopPlaceTest {

  @Test
  void flexibleStopPlaceIsRegisteredInTheEntityIndex() {
    NetexTestData data = new NetexTestData();
    data.addFlexibleStopPlace(1).addFlexibleArea(1);

    NetexEntitiesIndex index = data.build();

    assertNotNull(
      index.getFlexibleStopPlaceIndex().get("TST:FlexibleStopPlace:1")
    );
  }

  @Test
  void anUnassignedFlexibleStopPlaceIsNotResolvableByStopPointRef() {
    NetexTestData data = new NetexTestData();
    data.addFlexibleStopPlace(1).addFlexibleArea(1);

    NetexEntitiesIndex index = data.build();

    assertTrue(index.getFlexibleStopPlaceIdByStopPointRefIndex().isEmpty());
  }

  @Test
  void anAssignedFlexibleStopPlaceIsResolvableByStopPointRef() {
    NetexTestData data = new NetexTestData();
    data
      .addFlexibleStopPlace(1)
      .withScheduledStopPointRefs("TST:ScheduledStopPoint:1")
      .addFlexibleArea(1);

    NetexEntitiesIndex index = data.build();

    assertEquals(
      "TST:FlexibleStopPlace:1",
      index
        .getFlexibleStopPlaceIdByStopPointRefIndex()
        .get("TST:ScheduledStopPoint:1")
    );
  }

  @Test
  void oneFlexibleStopPlaceCanServeSeveralStopPoints() {
    NetexTestData data = new NetexTestData();
    data
      .addFlexibleStopPlace(1)
      .withScheduledStopPointRefs(
        "TST:ScheduledStopPoint:1",
        "TST:ScheduledStopPoint:2"
      )
      .addFlexibleArea(1);

    NetexEntitiesIndex index = data.build();

    assertEquals(
      "TST:FlexibleStopPlace:1",
      index
        .getFlexibleStopPlaceIdByStopPointRefIndex()
        .get("TST:ScheduledStopPoint:1")
    );
    assertEquals(
      "TST:FlexibleStopPlace:1",
      index
        .getFlexibleStopPlaceIdByStopPointRefIndex()
        .get("TST:ScheduledStopPoint:2")
    );
  }
}
