package no.entur.antu.stop.fetcher;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Predicate;

public abstract class AntuNetexEntityFetcher<R, S> implements NetexEntityFetcher<R, S> {

    protected static final long MAX_RETRY_ATTEMPTS = 3;

    protected static final Predicate<Throwable> is5xx =
            throwable -> throwable instanceof WebClientResponseException webClientResponseException && webClientResponseException.getStatusCode().is5xxServerError();

    protected final WebClient webClient;

    protected AntuNetexEntityFetcher(@Qualifier("stopPlaceWebClient") WebClient webClient) {
        this.webClient = webClient;
    }
}
