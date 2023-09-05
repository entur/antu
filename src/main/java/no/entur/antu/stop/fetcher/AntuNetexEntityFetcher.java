package no.entur.antu.stop.fetcher;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Predicate;

public abstract class AntuNetexEntityFetcher<R, S> implements NetexEntityFetcher<R, S> {

    protected final WebClient webClient;

    protected static final String ET_CLIENT_ID_HEADER = "ET-Client-ID";
    protected static final String ET_CLIENT_NAME_HEADER = "ET-Client-Name";

    protected static final long MAX_RETRY_ATTEMPTS = 3;

    protected static final Predicate<Throwable> is5xx =
            throwable -> throwable instanceof WebClientResponseException webClientResponseException && webClientResponseException.getStatusCode().is5xxServerError();

    protected AntuNetexEntityFetcher(String stopPlaceRegistryUrl,
                                     String clientId,
                                     String clientName) {
        this.webClient = WebClient.builder()
                .baseUrl(stopPlaceRegistryUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(ET_CLIENT_NAME_HEADER, clientName)
                .defaultHeader(ET_CLIENT_ID_HEADER, clientId)
                .build();
    }
}
