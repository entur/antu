package no.entur.antu.stop.fetcher;

import java.time.Duration;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.QuayId;
import org.rutebanken.netex.model.Quay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@Component
public class QuayFetcher extends AntuNetexEntityFetcher<Quay, QuayId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    QuayFetcher.class
  );

  protected QuayFetcher(
    @Qualifier("stopPlaceWebClient") WebClient stopPlaceWebClient
  ) {
    super(stopPlaceWebClient);
  }

  @Override
  public Quay tryFetch(QuayId quayId) {
    LOGGER.info(
      "Trying to fetch the Quay with id {}, from read API",
      quayId.id()
    );

    try {
      return this.webClient.get()
        .uri("/quays/{quayId}", quayId.id())
        .retrieve()
        .bodyToMono(Quay.class)
        .retryWhen(
          Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
        )
        .block();
    } catch (WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        LOGGER.warn("Quay with id : {} not found", quayId);
        return null;
      }
      throw new AntuException(
        "Failed fetching Quay for id " +
        quayId.id() +
        " due to " +
        e.getMessage()
      );
    }
  }
}
