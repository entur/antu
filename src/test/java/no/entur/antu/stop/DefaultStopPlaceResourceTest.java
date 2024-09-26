package no.entur.antu.stop;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

class DefaultStopPlaceResourceTest {

  private static final String NSR_QUAY_1 = "NSR:Quay:1";
  private static final String NSR_QUAY_2 = "NSR:Quay:2";
  private static final String NSR_QUAY_3 = "NSR:Quay:3";
  private static final String NSR_STOP_PLACE_1 = "NSR:StopPlace:1";
  private static final String NSR_STOP_PLACE_2 = "NSR:StopPlace:2";
  private static final String NSR_STOP_PLACE_NAME_1 = "Stop Place 123";
  private static final String NSR_STOP_PLACE_NAME_2 = "Stop Place 456";
  private static final AllVehicleModesOfTransportEnumeration NSR_STOP_PLACE_MODE_1 =
    AllVehicleModesOfTransportEnumeration.BUS;
  private static final AllVehicleModesOfTransportEnumeration NSR_STOP_PLACE_MODE_2 =
    AllVehicleModesOfTransportEnumeration.RAIL;

  private static final ObjectFactory netexFactory = new ObjectFactory();

  private NetexEntitiesIndex netexEntitiesIndex;

  @BeforeEach
  void setUp() {
    netexEntitiesIndex = new NetexEntitiesIndexImpl();

    netexEntitiesIndex
      .getQuayIndex()
      .putAll(
        List.of(
          new Quay()
            .withId(NSR_QUAY_1)
            .withVersion("1")
            .withName(new MultilingualString().withValue("Quay 123")),
          new Quay()
            .withId(NSR_QUAY_2)
            .withVersion("1")
            .withName(new MultilingualString().withValue("Quay 456")),
          new Quay()
            .withId(NSR_QUAY_3)
            .withVersion("1")
            .withName(new MultilingualString().withValue("Quay 4567"))
        )
      );

    Collection<JAXBElement<? extends Site_VersionStructure>> stopPlaces =
      List.of(
        netexFactory.createStopPlace_(
          new StopPlace()
            .withId(NSR_STOP_PLACE_1)
            .withVersion("1")
            .withName(new MultilingualString().withValue(NSR_STOP_PLACE_NAME_1))
            .withTransportMode(NSR_STOP_PLACE_MODE_1)
        ),
        netexFactory.createStopPlace_(
          new StopPlace()
            .withId(NSR_STOP_PLACE_2)
            .withVersion("1")
            .withName(new MultilingualString().withValue(NSR_STOP_PLACE_NAME_2))
            .withTransportMode(NSR_STOP_PLACE_MODE_2)
        )
      );
    netexEntitiesIndex
      .getSiteFrames()
      .add(
        new SiteFrame()
          .withStopPlaces(
            new StopPlacesInFrame_RelStructure().withStopPlace_(stopPlaces)
          )
      );

    netexEntitiesIndex
      .getStopPlaceIdByQuayIdIndex()
      .putAll(
        Map.of(
          NSR_QUAY_1,
          NSR_STOP_PLACE_1,
          NSR_QUAY_2,
          NSR_STOP_PLACE_2,
          NSR_QUAY_3,
          NSR_STOP_PLACE_2
        )
      );
  }

  @Test
  void getQuays() {
    DefaultStopPlaceResource defaultStopPlaceResource =
      new DefaultStopPlaceResource(() -> netexEntitiesIndex);
    Assertions.assertEquals(3, defaultStopPlaceResource.getQuays().size());
    Assertions.assertEquals(
      Map.of(
        new QuayId(NSR_QUAY_1),
        new SimpleQuay(null, new StopPlaceId(NSR_STOP_PLACE_1)),
        new QuayId(NSR_QUAY_2),
        new SimpleQuay(null, new StopPlaceId(NSR_STOP_PLACE_2)),
        new QuayId(NSR_QUAY_3),
        new SimpleQuay(null, new StopPlaceId(NSR_STOP_PLACE_2))
      ),
      defaultStopPlaceResource.getQuays()
    );
  }

  @Test
  void getStopPlaces() {
    DefaultStopPlaceResource defaultStopPlaceResource =
      new DefaultStopPlaceResource(() -> netexEntitiesIndex);
    Assertions.assertEquals(2, defaultStopPlaceResource.getStopPlaces().size());
    Assertions.assertEquals(
      Map.of(
        new StopPlaceId(NSR_STOP_PLACE_1),
        new SimpleStopPlace(
          NSR_STOP_PLACE_NAME_1,
          new TransportModeAndSubMode(NSR_STOP_PLACE_MODE_1, null)
        ),
        new StopPlaceId(NSR_STOP_PLACE_2),
        new SimpleStopPlace(
          NSR_STOP_PLACE_NAME_2,
          new TransportModeAndSubMode(NSR_STOP_PLACE_MODE_2, null)
        )
      ),
      defaultStopPlaceResource.getStopPlaces()
    );
  }
}
