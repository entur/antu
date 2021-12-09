package no.entur.antu.stop;

import no.entur.antu.exception.AntuException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class StopPlaceResource {

    private static final int MAX_DOWNLOAD_BUFFER_SIZE = 10 * 1024 * 1024;

    private final WebClient webClient;

    public StopPlaceResource(String stopIdEndPoint, WebClient.Builder webClientBuilder) {
        webClientBuilder = webClientBuilder.exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_DOWNLOAD_BUFFER_SIZE))
                .build());
        this.webClient = webClientBuilder.baseUrl(stopIdEndPoint).build();
    }

    public Set<String> getQuayIds() {
        return getIds("/quay?includeFuture=true");
    }

    public Set<String> getStopPlaceIds() {
        return getIds("/stop_place?includeFuture=true");
    }

    private Set<String> getIds(String uri) {
        // the payload is less than 10Mb, so the ids can be retrieved without streaming
        String allIds = webClient.get()
                .uri(uri)
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (allIds == null) {
            throw new AntuException("The endpoint " + uri + " did not return any id");
        }

        return Arrays.stream(allIds.split("\n")).collect(Collectors.toSet());
    }
}
