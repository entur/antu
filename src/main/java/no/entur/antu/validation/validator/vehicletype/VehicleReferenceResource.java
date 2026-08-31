package no.entur.antu.validation.validator.vehicletype;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

public class VehicleReferenceResource {

  private static final long MAX_RETRY_ATTEMPTS = 3;

  private final WebClient webClient;

  public VehicleReferenceResource(
    @Qualifier("vehicleRegistryWebClient") WebClient vehicleRegistryWebClient
  ) {
    this.webClient = vehicleRegistryWebClient.mutate().build();
  }

  public Set<String> getVehicleAndVehicleTypesRef() {
    return webClient
      .get()
      .uri("/validation/netexids")
      .retrieve()
      .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
      .retryWhen(
        Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
      )
      .flatMapMany(Flux::fromIterable)
      .collect(Collectors.toSet())
      .block();
  }

  protected static final Predicate<Throwable> is5xx = throwable ->
    throwable instanceof WebClientResponseException webClientResponseException &&
    webClientResponseException.getStatusCode().is5xxServerError();
}
