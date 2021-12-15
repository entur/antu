package no.entur.antu.stop;

import no.entur.antu.exception.AntuException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Retrieve the NeTEx ids of all stop places and quays in the stop registry.
 * This includes both the official stop and quay ids (NSR:StopPlace:* and NSR:Quay:*) and the "local references" that are unofficial, provider-specific ids (example: RUT:StopPlace:* and RUT:Quay:*)
 */
public class DefaultStopPlaceResource implements StopPlaceResource {

    private static final int MAX_DOWNLOAD_BUFFER_SIZE = 10 * 1024 * 1024;

    private final WebClient webClient;

    public DefaultStopPlaceResource(String stopIdEndPoint, WebClient.Builder webClientBuilder) {
        webClientBuilder = webClientBuilder.exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_DOWNLOAD_BUFFER_SIZE))
                .build());
        this.webClient = webClientBuilder.baseUrl(stopIdEndPoint).build();
    }

    @Override
    public Set<String> getQuayIds() {
        List<String> nsrIds = getNetexIds("/id/quay?includeFuture=true");
        List<String> localReferences = getNetexIds("/local_reference/quay?recordsPerRoundTrip=500000&includeFuture=true");
        Set<String> ids = new HashSet<>(nsrIds.size() + localReferences.size());
        ids.addAll(nsrIds);
        ids.addAll(localReferences);
        return ids;
    }

    @Override
    public Set<String> getStopPlaceIds() {
        List<String> nsrIds = getNetexIds("/id/stop_place?includeFuture=true");
        List<String> localReferences = getNetexIds("/local_reference/stop_place?recordsPerRoundTrip=220000&includeFuture=true");
        Set<String> ids = new HashSet<>(nsrIds.size() + localReferences.size());
        ids.addAll(nsrIds);
        ids.addAll(localReferences);
        return ids;
    }

    /**
     * Return the list of NetEX Ids.
     * The id service and local_reference service return a plain-text payload containing one NeTEx ID per line.
     *
     * @param uri
     * @return
     */
    private List<String> getNetexIds(String uri) {
        String allIds = getPayload(uri);
        if (allIds == null) {
            throw new AntuException("The endpoint " + uri + " did not return any id");
        }
        return Arrays.asList(allIds.split("\n"));
    }

    private String getPayload(String uri) {
        // the payload is less than 10Mb, ids can be retrieved without streaming
        return webClient.get()
                .uri(uri)
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
