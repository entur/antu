package no.entur.antu.stop.fetcher;

import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.model.QuayId;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StopPlaceForQuayIdFetcher extends AntuNetexEntityFetcher<StopPlace, QuayId> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopPlaceForQuayIdFetcher.class);

    protected StopPlaceForQuayIdFetcher(@Value("${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}") String stopPlaceRegistryUrl,
                                        @Value("${http.client.name:antu}") String clientName,
                                        @Value("${http.client.id:antu}") String clientId) {
        super(stopPlaceRegistryUrl, clientId, clientName);
    }

    @Override
    public StopPlace tryFetch(QuayId quayId) {

        LOGGER.info("Trying to fetch the stop place with quay id {}, from read API", quayId.id());

        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}/stop-place", quayId.id())
                    .retrieve()
                    .bodyToMono(StopPlace.class)
                    .block();
        } catch (Exception e) {
            throw new AntuException("Could not find StopPlace for quay id " + quayId.id() + " due to " + e.getMessage());
        }
    }
}
