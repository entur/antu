package no.entur.antu.organisation;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

/**
 * REST-client to access V3 of Organisation Register REST-service.
 */
public class OrganisationV3Resource {

  private static final long MAX_RETRY_ATTEMPTS = 3;
  private static final Logger log = LoggerFactory.getLogger(
    OrganisationV3Resource.class
  );

  private final WebClient webClient;

  public OrganisationV3Resource(@Qualifier("orgRegisterV3Client") WebClient orgRegisterWebClient) {
    this.webClient = orgRegisterWebClient.mutate().build();
  }

  public Collection<OrganisationV3> getOrganisations() {
    return webClient
      .get()
      .uri("/aliases")
      .retrieve()
      .bodyToFlux(OrganisationV3.class)
      .retryWhen(
        Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
      )
      .collectList()
      .block();
  }

  protected static final Predicate<Throwable> is5xx = throwable ->
    throwable instanceof WebClientResponseException webClientResponseException &&
    webClientResponseException.getStatusCode().is5xxServerError();
}
