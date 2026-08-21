package no.entur.antu.stop.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class TiamatStopPlaceRegistryTest {

  private static final String QUAY_ID = "NSR:Quay:111871";
  private static final String STOP_PLACE_ID = "NSR:StopPlace:64572";

  /**
   * As the read API returns it: a bare StopPlace element, which the NeTEx model does not declare as a
   * root element.
   */
  private static final String STOP_PLACE_XML =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <StopPlace xmlns="http://www.netex.org.uk/netex" version="2" id="NSR:StopPlace:64572">
        <Name lang="nor">Jektevik terminal</Name>
        <TransportMode>water</TransportMode>
        <WaterSubmode>internationalCarFerry</WaterSubmode>
        <quays>
            <Quay version="2" id="NSR:Quay:111871">
                <Centroid>
                    <Location>
                        <Longitude>5.308161</Longitude>
                        <Latitude>60.392249</Latitude>
                    </Location>
                </Centroid>
            </Quay>
        </quays>
    </StopPlace>
    """;

  private final List<String> requestedPaths = new ArrayList<>();
  private final Set<String> notFoundIds = new HashSet<>();

  @Test
  void aQuayIsFoundThroughItsStopPlace() {
    TiamatStopPlaceRegistry registry = registryReturning(
      HttpStatus.OK,
      STOP_PLACE_XML
    );

    RegisteredStopPlace registered = registry.findByQuayId(new QuayId(QUAY_ID));

    assertNotNull(registered);
    assertEquals(STOP_PLACE_ID, registered.id().id());
    assertEquals("Jektevik terminal", registered.stopPlace().name());
    assertEquals(
      "water",
      registered.stopPlace().transportModeAndSubMode().mode().value()
    );
    assertEquals(
      "/quays/NSR:Quay:111871/stop-place",
      requestedPaths.getFirst()
    );
  }

  @Test
  void theQuaysOfTheStopPlaceAreReturnedWithTheirCoordinates() {
    TiamatStopPlaceRegistry registry = registryReturning(
      HttpStatus.OK,
      STOP_PLACE_XML
    );

    RegisteredStopPlace registered = registry.findById(
      new StopPlaceId(STOP_PLACE_ID)
    );

    assertNotNull(registered);
    assertEquals("/stop-places/NSR:StopPlace:64572", requestedPaths.getFirst());
    assertEquals(1, registered.quays().size());
    assertEquals(
      60.392249,
      registered.quays().get(new QuayId(QUAY_ID)).quayCoordinates().latitude()
    );
  }

  @Test
  void anUnknownQuayIsRememberedAndAskedForOnlyOnce() {
    TiamatStopPlaceRegistry registry = registryReturning(
      HttpStatus.NOT_FOUND,
      ""
    );

    assertNull(registry.findByQuayId(new QuayId(QUAY_ID)));
    assertNull(registry.findByQuayId(new QuayId(QUAY_ID)));

    assertTrue(notFoundIds.contains(QUAY_ID));
    assertEquals(1, requestedPaths.size());
  }

  /**
   * A registry that is down must not fail the validation and must not be remembered as a 404: the
   * caller reports the reference as unresolved, as it did before the lookup existed, and the next
   * dataset referencing the id asks again.
   */
  @Test
  void aRegistryErrorIsNotRememberedAsMissing() {
    TiamatStopPlaceRegistry registry = registryReturning(
      HttpStatus.SERVICE_UNAVAILABLE,
      ""
    );

    assertNull(registry.findByQuayId(new QuayId(QUAY_ID)));

    assertTrue(notFoundIds.isEmpty());
  }

  /**
   * A response that does not parse is a broken registry, not a broken dataset: the caller has to be able
   * to report the reference as unresolved rather than fail the whole validation.
   */
  @Test
  void aResponseThatDoesNotParseIsNotAnError() {
    TiamatStopPlaceRegistry registry = registryReturning(
      HttpStatus.OK,
      "<StopPlace"
    );

    assertNull(registry.findByQuayId(new QuayId(QUAY_ID)));
    assertTrue(notFoundIds.isEmpty());
  }

  private TiamatStopPlaceRegistry registryReturning(
    HttpStatus status,
    String body
  ) {
    WebClient webClient = WebClient
      .builder()
      .baseUrl("http://stop-place-registry")
      .exchangeFunction(request -> {
        requestedPaths.add(decodedPath(request));
        return Mono.just(
          ClientResponse
            .create(status)
            .header("Content-Type", MediaType.APPLICATION_XML_VALUE)
            .body(body)
            .build()
        );
      })
      .build();
    return new TiamatStopPlaceRegistry(
      webClient,
      new InMemoryNotFoundIdCache()
    );
  }

  private static String decodedPath(ClientRequest request) {
    return java.net.URLDecoder.decode(
      request.url().getRawPath(),
      StandardCharsets.UTF_8
    );
  }

  private class InMemoryNotFoundIdCache implements NotFoundIdCache {

    @Override
    public boolean contains(String id) {
      return notFoundIds.contains(id);
    }

    @Override
    public void remember(String id) {
      notFoundIds.add(id);
    }
  }
}
