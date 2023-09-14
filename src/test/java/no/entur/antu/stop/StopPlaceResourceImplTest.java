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

class StopPlaceResourceImplTest {

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

        StopPlaceResourceImpl stopPlaceResourceImpl = mock(StopPlaceResourceImpl.class);
        when(stopPlaceResourceImpl.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);
        when(stopPlaceResourceImpl.getQuayIds()).thenCallRealMethod();

        Set<String> quayIds = stopPlaceResourceImpl.getQuayIds();

        verify(stopPlaceResourceImpl).getQuayIds();
        verify(stopPlaceResourceImpl).getNetexEntitiesIndex();

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

        StopPlaceResourceImpl stopPlaceResourceImpl = mock(StopPlaceResourceImpl.class);
        when(stopPlaceResourceImpl.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);
        when(stopPlaceResourceImpl.getStopPlaceIds()).thenCallRealMethod();

        Set<String> stopPlaceIds = stopPlaceResourceImpl.getStopPlaceIds();

        verify(stopPlaceResourceImpl).getStopPlaceIds();
        verify(stopPlaceResourceImpl).getNetexEntitiesIndex();

        assertThat(stopPlaceIds.size(), is(2));
    }
}