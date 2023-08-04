package no.entur.antu.stop.fetcher;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.StopPlace;

public class StopPlaceFetcher extends AntuNetexEntityFetcher<StopPlace, String> {

    @Override
    public StopPlace tryFetch(String quayId) {
        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}/stop-place", quayId)
                    .retrieve()
                    .bodyToMono(StopPlace.class)
                    .block();
        } catch (Exception e) {
            throw new AntuException("Could not find StopPlace for quay id " + quayId + " due to " + e.getMessage());
        }
    }
}
