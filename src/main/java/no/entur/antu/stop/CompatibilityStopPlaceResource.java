package no.entur.antu.stop;

import no.entur.antu.exception.AntuException;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Temporary implementation using the original Tiamat services.
 * Retrieve the NeTEx ids of all stop places and quays in the stop registry.
 * This includes both the official stop and quay ids (NSR:StopPlace:* and NSR:Quay:*) and the "local references" that are unofficial, provider-specific ids (example: RUT:StopPlace:* and RUT:Quay:*)
 *
 */
public class CompatibilityStopPlaceResource implements  StopPlaceResource{

    private static final int MAX_DOWNLOAD_BUFFER_SIZE = 20 * 1024 * 1024;

    private final WebClient webClient;

    public CompatibilityStopPlaceResource(String stopIdEndPoint, WebClient.Builder webClientBuilder) {
        webClientBuilder = webClientBuilder.exchangeStrategies(ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(MAX_DOWNLOAD_BUFFER_SIZE))
                .build());
        this.webClient = webClientBuilder.baseUrl(stopIdEndPoint).build();
    }

    public Set<String> getQuayIds() {
        List<String> nsrIds = getNsrIds("/id/quay?includeFuture=true");
        List<String> localReferences = getLocalReferences("/mapping/quay?recordsPerRoundTrip=500000&includeFuture=true");
        Set<String> ids = new HashSet<>(nsrIds.size() + localReferences.size());
        ids.addAll(nsrIds);
        ids.addAll(localReferences);
        return ids;
    }

    public Set<String> getStopPlaceIds() {
        List<String> nsrIds = getNsrIds("/id/stop_place?includeFuture=true");
        List<String> localReferences = getLocalReferences("/mapping/stop_place?recordsPerRoundTrip=220000&includeFuture=true");
        Set<String> ids = new HashSet<>(nsrIds.size() + localReferences.size());
        ids.addAll(nsrIds);
        ids.addAll(localReferences);
        return ids;
    }

    /**
     * Return the list of NSR Ids.
     * The id service returns a plain-text payload containing one NSR ID per line.
     * @param uri
     * @return
     */
    private List<String> getNsrIds(String uri) {
        String allIds = getPayload(uri);
        if (allIds == null) {
            throw new AntuException("The endpoint " + uri + " did not return any id");
        }
        return Arrays.stream(allIds.split("\n")).collect(Collectors.toList());
    }

    /**
     * Return the list of local references.
     * The mapping service returns a plain-text payload containing for each line one pair (Local reference, NSR id) followed by the validity period.
     * Only the first field (Local reference) is collected here since the id is already collected in {@link #getNsrIds(String)}
     * @param uri
     * @return
     */
    private List<String> getLocalReferences(String uri) {
        String allMappings = getPayload(uri);
        if (allMappings == null) {
            throw new AntuException("The endpoint " + uri + " did not return any id");
        }
        return Arrays.stream(allMappings.split("\n"))
                .map(line -> line.substring(0, line.indexOf(',')))
                .collect(Collectors.toList());
    }

    private String getPayload(String uri) {
        // the payload is less than 20Mb, so the ids can be retrieved without streaming
        return webClient.get()
                .uri(uri)
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
