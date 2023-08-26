package no.entur.antu.stop.fetcher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.function.Predicate;

public abstract class AntuNetexEntityFetcher<R, S> implements NetexEntityFetcher<R, S> {

    protected final WebClient webClient;

    @Value("${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}")
    protected String stopPlaceRegistryUrl;

    protected static final String ET_CLIENT_ID_HEADER = "ET-Client-ID";
    protected static final String ET_CLIENT_NAME_HEADER = "ET-Client-Name";

    protected static final long MAX_RETRY_ATTEMPTS = 3;

    @Value("${http.client.name:antu}")
    protected String clientName;

    @Value("${http.client.id:antu}")
    protected String clientId;

    protected static final Predicate<Throwable> is5xx =
            throwable -> throwable instanceof WebClientResponseException webClientResponseException && webClientResponseException.getStatusCode().is5xxServerError();

    protected AntuNetexEntityFetcher() {
        this.webClient = WebClient.builder()
                .baseUrl(stopPlaceRegistryUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(ET_CLIENT_NAME_HEADER, clientName)
                .defaultHeader(ET_CLIENT_ID_HEADER, clientId)
                .build();
    }
}
