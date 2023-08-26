package no.entur.antu.stop.fetcher;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.Quay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.util.retry.Retry;

import java.time.Duration;

public class QuayFetcher extends AntuNetexEntityFetcher<Quay, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuayFetcher.class);

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
