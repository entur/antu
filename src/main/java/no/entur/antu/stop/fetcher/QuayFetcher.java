package no.entur.antu.stop.fetcher;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.Quay;

public class QuayFetcher extends AntuNetexEntityFetcher<Quay, String> {

    @Override
    public Quay tryFetch(String quayId) {
        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}", quayId)
                    .retrieve()
                    .bodyToMono(Quay.class)
                    .block();
        } catch (Exception e) {
            throw new AntuException("Could not find Quay for id " + quayId + " due to " + e.getMessage());
        }
    }
}
