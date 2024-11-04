package no.entur.antu.stop.fetcher;

import java.time.Duration;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.model.QuayId;
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
public class StopPlaceForQuayIdFetcher
  extends AntuNetexEntityFetcher<StopPlace, QuayId> {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPlaceForQuayIdFetcher.class
  );

  public static boolean isValidNSRQuay(QuayId quayId) {
    return quayId != null && quayId.id().startsWith("NSR:Quay:");
  }

  protected StopPlaceForQuayIdFetcher(
    @Qualifier("stopPlaceWebClient") WebClient stopPlaceWebClient
  ) {
    super(stopPlaceWebClient);
  }

  @Override
  public StopPlace tryFetch(QuayId quayId) {
    if (!isValidNSRQuay(quayId)) {
      LOGGER.warn(
        "Ignored fetching stop place for invalid NSR Quay with id : {}",
        quayId
      );
      return null;
    }

    LOGGER.info(
      "Trying to fetch the stop place with quay id {}, from read API",
      quayId.id()
    );

    try {
      return this.webClient.get()
        .uri("/quays/{quayId}/stop-place", quayId.id())
        .retrieve()
        .bodyToMono(StopPlace.class)
        .retryWhen(
          Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
        )
        .block();
    } catch (WebClientResponseException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        LOGGER.warn("Stop place with quay id : {} not found", quayId);
        return null;
      }
      throw new AntuException(
        "Failed fetching StopPlace for quay id " +
        quayId.id() +
        " due to " +
        e.getMessage()
      );
    }
  }
}
