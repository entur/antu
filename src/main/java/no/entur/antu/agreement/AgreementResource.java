package no.entur.antu.agreement;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class AgreementResource {

    private static final long MAX_RETRY_ATTEMPTS = 3;

    private final WebClient webClient;

    public AgreementResource(@Qualifier("agreementRegisterWebClient") WebClient agreementRegisterWebClient) {
        this.webClient = agreementRegisterWebClient.mutate().build();
    }

    public Collection<Agreement> getAuthorityIds() {
        List<Agreement> authorities = webClient
            .get()
            .uri("/AUTHORITY")
            .retrieve()
            .bodyToFlux(Agreement.class)
            .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx))
            .collectList()
            .block();

        if (authorities == null || authorities.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(authorities);
    }

    public Collection<Agreement> getOperatorIds() {
        List<Agreement> operators = webClient
            .get()
            .uri("/OPERATOR")
            .retrieve()
            .bodyToFlux(Agreement.class)
            .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx))
            .collectList()
            .block();

        if (operators == null || operators.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(operators);
    }

    protected static final Predicate<Throwable> is5xx = throwable ->
            throwable instanceof WebClientResponseException webClientResponseException &&
                    webClientResponseException.getStatusCode().is5xxServerError();
}
