package no.entur.antu.stop.fetcher;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.Quay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;

@Component
public class QuayFetcher extends AntuNetexEntityFetcher<Quay, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuayFetcher.class);

    protected QuayFetcher(@Value("${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}") String stopPlaceRegistryUrl,
                          @Value("${http.client.name:antu}") String clientName,
                          @Value("${http.client.id:antu}") String clientId) {
        super(stopPlaceRegistryUrl, clientId, clientName);
    }

    @Override
    public Quay tryFetch(String quayId) {

        LOGGER.info("Trying to fetch the Quay with id {}, from read API", quayId);

        try {
            return this.webClient.get()
                    .uri("/quays/{quayId}", quayId)
                    .retrieve()
                    .bodyToMono(Quay.class)
                    .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx))
                    .block();
        } catch (Exception e) {
            throw new AntuException("Could not find Quay for id " + quayId + " due to " + e.getMessage());
        }
    }
}
