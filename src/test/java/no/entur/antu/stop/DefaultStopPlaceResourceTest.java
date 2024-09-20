package no.entur.antu.stop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import no.entur.antu.model.SimpleQuay;
import no.entur.antu.model.SimpleStopPlace;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceId;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

class DefaultStopPlaceResourceTest {

  private static final ObjectFactory netexFactory = new ObjectFactory();

  @Test
  void getQuays() {
    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();

    netexEntitiesIndex
      .getQuayIndex()
      .putAll(
        List.of(
          new Quay()
            .withId("NSR:Quay:123")
            .withVersion("1")
            .withName(new MultilingualString().withValue("Quay 123")),
          new Quay()
            .withId("NSR:Quay:456")
            .withVersion("1")
            .withName(new MultilingualString().withValue("Quay 456"))
        )
      );

    netexEntitiesIndex
      .getQuayIndex()
      .putAll(
        List.of(
          new Quay()
            .withId("NSR:Quay:123")
            .withVersion("2")
            .withName(new MultilingualString().withValue("Quay 123")),
          new Quay()
            .withId("NSR:Quay:4567")
            .withVersion("1")
            .withName(new MultilingualString().withValue("Quay 4567"))
        )
      );

    netexEntitiesIndex
      .getStopPlaceIdByQuayIdIndex()
      .putAll(
        Map.of(
          "NSR:Quay:123",
          "NSR:StopPlace:123",
          "NSR:Quay:456",
          "NSR:StopPlace:456",
          "NSR:Quay:4567",
          "NSR:StopPlace:456"
        )
      );

    DefaultStopPlaceResource defaultStopPlaceResource = mock(
      DefaultStopPlaceResource.class
    );
    when(defaultStopPlaceResource.getNetexEntitiesIndex())
      .thenReturn(netexEntitiesIndex);
    when(defaultStopPlaceResource.getQuays()).thenCallRealMethod();

    Map<QuayId, SimpleQuay> quayIds = defaultStopPlaceResource.getQuays();

    verify(defaultStopPlaceResource).getQuays();
    verify(defaultStopPlaceResource, times(4)).getNetexEntitiesIndex();

    assertThat(quayIds.size(), is(3));
  }

  @Test
  void getStopPlaces() {
    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
    Collection<JAXBElement<? extends Site_VersionStructure>> stopPlaces =
      List.of(
        netexFactory.createStopPlace_(
          new StopPlace()
            .withId("NSR:StopPlace:123")
            .withVersion("1")
            .withName(new MultilingualString().withValue("Stop Place 123"))
        ),
        netexFactory.createStopPlace_(
          new StopPlace()
            .withId("NSR:StopPlace:456")
            .withVersion("1")
            .withName(new MultilingualString().withValue("Stop Place 456"))
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

    DefaultStopPlaceResource defaultStopPlaceResource = mock(
      DefaultStopPlaceResource.class
    );
    when(defaultStopPlaceResource.getNetexEntitiesIndex())
      .thenReturn(netexEntitiesIndex);
    when(defaultStopPlaceResource.getStopPlaces()).thenCallRealMethod();

    Map<StopPlaceId, SimpleStopPlace> stopPlaceIds =
      defaultStopPlaceResource.getStopPlaces();

    verify(defaultStopPlaceResource).getStopPlaces();
    verify(defaultStopPlaceResource).getNetexEntitiesIndex();

    assertThat(stopPlaceIds.size(), is(2));
  }
}
