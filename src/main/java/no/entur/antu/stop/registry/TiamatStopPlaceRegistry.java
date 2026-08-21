package no.entur.antu.stop.registry;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.xml.transform.stream.StreamSource;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

/**
 * Stop place lookups against the read-only API of the National Stop Place Registry.
 */
public class TiamatStopPlaceRegistry implements StopPlaceRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    TiamatStopPlaceRegistry.class
  );

  private static final String STOP_PLACE_BY_QUAY_ID_PATH =
    "/quays/{id}/stop-place";
  private static final String STOP_PLACE_BY_ID_PATH = "/stop-places/{id}";

  private static final long MAX_RETRY_ATTEMPTS = 2;
  private static final Duration RETRY_BACKOFF = Duration.ofMillis(500);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final WebClient webClient;
  private final NotFoundIdCache notFoundIds;
  private final JAXBContext jaxbContext;

  public TiamatStopPlaceRegistry(
    WebClient webClient,
    NotFoundIdCache notFoundIds
  ) {
    this.webClient = webClient;
    this.notFoundIds = notFoundIds;
    this.jaxbContext = stopPlaceJaxbContext();
  }

  @Override
  @Nullable
  public RegisteredStopPlace findByQuayId(QuayId quayId) {
    return find(quayId.id(), STOP_PLACE_BY_QUAY_ID_PATH);
  }

  @Override
  @Nullable
  public RegisteredStopPlace findById(StopPlaceId stopPlaceId) {
    return find(stopPlaceId.id(), STOP_PLACE_BY_ID_PATH);
  }

  @Nullable
  private RegisteredStopPlace find(String id, String path) {
    if (notFoundIds.contains(id)) {
      return null;
    }
    byte[] body = get(id, path);
    if (body == null) {
      return null;
    }
    StopPlace stopPlace = unmarshal(id, body);
    return stopPlace == null ? null : toRegisteredStopPlace(stopPlace);
  }

  /**
   * Returns null both when the registry does not have the id and when it could not be reached. A
   * registry that is down must not fail the validation: the caller then reports the reference as
   * unresolved, which is what it did before this lookup existed. Only a 404 is remembered, so an
   * outage is retried rather than cached.
   */
  @Nullable
  private byte[] get(String id, String path) {
    try {
      return webClient
        .get()
        .uri(path, id)
        .retrieve()
        .bodyToMono(byte[].class)
        .retryWhen(
          Retry
            .backoff(MAX_RETRY_ATTEMPTS, RETRY_BACKOFF)
            .filter(TiamatStopPlaceRegistry::isServerError)
        )
        .block(REQUEST_TIMEOUT);
    } catch (WebClientResponseException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
        notFoundIds.remember(id);
        return null;
      }
      LOGGER.warn(
        "Stop place registry returned {} for {}",
        e.getStatusCode(),
        id
      );
      return null;
    } catch (Exception e) {
      LOGGER.warn("Failed looking up {} in the stop place registry", id, e);
      return null;
    }
  }

  private static boolean isServerError(Throwable throwable) {
    return (
      throwable instanceof WebClientResponseException webClientResponseException &&
      webClientResponseException.getStatusCode().is5xxServerError()
    );
  }

  private static RegisteredStopPlace toRegisteredStopPlace(
    StopPlace stopPlace
  ) {
    StopPlaceId stopPlaceId = new StopPlaceId(stopPlace.getId());
    SimpleStopPlace simpleStopPlace = new SimpleStopPlace(
      stopPlace.getName() == null ? "" : stopPlace.getName().getValue(),
      TransportModeAndSubMode.of(stopPlace)
    );

    Map<QuayId, SimpleQuay> quays = new HashMap<>();
    if (stopPlace.getQuays() != null) {
      for (JAXBElement<?> element : stopPlace.getQuays().getQuayRefOrQuay()) {
        if (
          element.getValue() instanceof Quay quay &&
          QuayId.isValid(quay.getId())
        ) {
          quays.put(
            new QuayId(quay.getId()),
            new SimpleQuay(QuayCoordinates.of(quay), stopPlaceId)
          );
        }
      }
    }
    return new RegisteredStopPlace(stopPlaceId, simpleStopPlace, quays);
  }

  /**
   * Null on a response that does not parse, for the same reason a 5xx is: whatever is wrong with the
   * registry, the answer here is that the id could not be confirmed, not that the dataset is invalid.
   */
  @Nullable
  private StopPlace unmarshal(String id, byte[] body) {
    try {
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
      // The registry returns a bare StopPlace element, which the NeTEx model does not declare as a
      // root element, so the type has to be given here.
      return unmarshaller
        .unmarshal(
          new StreamSource(new ByteArrayInputStream(body)),
          StopPlace.class
        )
        .getValue();
    } catch (JAXBException e) {
      LOGGER.warn(
        "Could not parse the stop place the registry returned for {}",
        id,
        e
      );
      return null;
    }
  }

  private static JAXBContext stopPlaceJaxbContext() {
    try {
      return JAXBContext.newInstance(StopPlace.class);
    } catch (JAXBException e) {
      throw new AntuException("Cannot create the stop place JAXB context", e);
    }
  }
}
