package no.entur.antu.stop;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

class CurrentStopPlaceResourceTest {

    @Test
    void getQuayIds() {
        NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();

        netexEntitiesIndex.getQuayIndex().putAll(
                List.of(new Quay().withId("NSR:Quay:123").withVersion("1"),
                        new Quay().withId("NSR:Quay:456").withVersion("1"))
        );

        netexEntitiesIndex.getQuayIndex().putAll(
                List.of(new Quay().withId("NSR:Quay:123").withVersion("2"),
                        new Quay().withId("NSR:Quay:4567").withVersion("1"))
        );

        CurrentStopPlaceResource currentStopPlaceResource = mock(CurrentStopPlaceResource.class);
        when(currentStopPlaceResource.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);
        when(currentStopPlaceResource.getQuayIds()).thenCallRealMethod();

        Set<String> quayIds = currentStopPlaceResource.getQuayIds();

        verify(currentStopPlaceResource).getQuayIds();
        verify(currentStopPlaceResource).getNetexEntitiesIndex();

        assertThat(quayIds.size(), is(3));
    }

    @Test
    void getStopPlaceIds() {
        NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();

        netexEntitiesIndex.getSiteFrames().add(new SiteFrame().withStopPlaces(
                new StopPlacesInFrame_RelStructure().withStopPlace(
                        List.of(new StopPlace().withId("NSR:StopPlace:123").withVersion("1"),
                                new StopPlace().withId("NSR:StopPlace:456").withVersion("1"))
                )
        ));

        CurrentStopPlaceResource currentStopPlaceResource = mock(CurrentStopPlaceResource.class);
        when(currentStopPlaceResource.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);
        when(currentStopPlaceResource.getStopPlaceIds()).thenCallRealMethod();

        Set<String> stopPlaceIds = currentStopPlaceResource.getStopPlaceIds();

        verify(currentStopPlaceResource).getStopPlaceIds();
        verify(currentStopPlaceResource).getNetexEntitiesIndex();

        assertThat(stopPlaceIds.size(), is(2));
    }
}