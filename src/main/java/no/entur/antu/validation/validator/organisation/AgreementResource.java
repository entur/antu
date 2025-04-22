package no.entur.antu.validation.validator.organisation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AgreementResource {
    private static final long MAX_RETRY_ATTEMPTS = 3;

    private final WebClient webClient;

    public AgreementResource(
            @Qualifier("agreementRegistryWebClient") WebClient agreementRegistryWebClient
    ) {
        this.webClient = agreementRegistryWebClient.mutate().build();
    }

    public Set<String> getOrganisationAliases() {
        return webClient
                .get()
                .uri("/adapter/transmodel/ids")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Agreement>>() {})
                .retryWhen(
                        Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx)
                )
                .flatMapMany(Flux::fromIterable)
                .flatMapIterable(agreement -> {
                    List<String> aliases = new ArrayList<>();
                    aliases.addAll(agreement.getAliases());
                    aliases.addAll(agreement.getRoleIds());
                    return aliases;
                })
                .collect(Collectors.toSet()).block();
    }

    protected static final Predicate<Throwable> is5xx = throwable ->
            throwable instanceof WebClientResponseException webClientResponseException &&
                    webClientResponseException.getStatusCode().is5xxServerError();
}
