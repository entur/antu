package no.entur.antu.stop.fetcher;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StopPlaceFetcher extends AntuNetexEntityFetcher<StopPlace, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopPlaceFetcher.class);

    protected StopPlaceFetcher(@Value("${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}") String stopPlaceRegistryUrl,
                               @Value("${http.client.name:antu}") String clientName,
                               @Value("${http.client.id:antu}") String clientId) {
        super(stopPlaceRegistryUrl, clientId, clientName);
    }

    @Override
    public StopPlace tryFetch(String stopPlaceId) {

        LOGGER.info("Trying to fetch the stop place with id {}, from read API", stopPlaceId);

        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}/stop-place", stopPlaceId)
                    .retrieve()
                    .bodyToMono(StopPlace.class)
                    .block();
        } catch (Exception e) {
            throw new AntuException("Could not find StopPlace for quay id " + stopPlaceId + " due to " + e.getMessage());
        }
    }
}
