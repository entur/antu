package no.entur.antu.stop.fetcher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class AntuNetexEntityFetcher<R, S> implements NetexEntityFetcher<R, S> {

    protected final WebClient webClient;

    @Value("${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}")
    protected String stopPlaceRegistryUrl;

    protected static final String ET_CLIENT_ID_HEADER = "ET-Client-ID";
    protected static final String ET_CLIENT_NAME_HEADER = "ET-Client-Name";

    @Value("${http.client.name:damu}")
    protected String clientName;

    @Value("${http.client.id:damu}")
    protected String clientId;

    protected AntuNetexEntityFetcher() {
        this.webClient = WebClient.builder()
                .baseUrl(stopPlaceRegistryUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
                .defaultHeader(ET_CLIENT_NAME_HEADER, clientName)
                .defaultHeader(ET_CLIENT_ID_HEADER, clientId)
                .build();
    }
}
