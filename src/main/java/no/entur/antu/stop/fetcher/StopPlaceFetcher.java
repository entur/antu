package no.entur.antu.stop.fetcher;

import java.time.Duration;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.StopPlaceId;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

@Component
public class StopPlaceFetcher
  extends AntuNetexEntityFetcher<StopPlace, StopPlaceId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPlaceFetcher.class
  );

  protected StopPlaceFetcher(
    @Qualifier("stopPlaceWebClient") WebClient stopPlaceWebClient
  ) {
    super(stopPlaceWebClient);
  }

  @Override
  public StopPlace tryFetch(StopPlaceId stopPlaceId) {
    LOGGER.info(
      "Trying to fetch the stop place with id {}, from read API",
      stopPlaceId.id()
    );

    try {
      return this.webClient.get()
        .uri("/stop-places/{stopPlaceId}", stopPlaceId.id())
        .retrieve()
        .bodyToMono(StopPlace.class)
        .retryWhen(
          Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
        )
        .block();
    } catch (WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        LOGGER.warn("Stop place with id : {} not found", stopPlaceId);
        return null;
      }
      throw new AntuException(
        "Failed fetching StopPlace for id " +
        stopPlaceId.id() +
        " due to " +
        e.getMessage()
      );
    }
  }
}
