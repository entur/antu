package no.entur.antu.netex.test.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.StopPlace;

/**
 * A StopPlaceRepository backed by plain maps, mutable after construction.
 *
 * <p>The named fixtures build the scenarios the validators care about: the bus/coach/taxi
 * substitution rules in MismatchedTransportModeSubModeValidator, and the absent-reference-data
 * case that several validators silently skip on.
 *
 * <p>An explicitly put transport mode, coordinate or name wins over the one derived from the
 * StopPlace and Quay, so a fixture can be adjusted for a single quay without rebuilding it.
 */
public class TestStopPlaceRepository implements StopPlaceRepository {

  private final Map<StopPlaceId, StopPlace> stopPlaces = new HashMap<>();
  private final Map<QuayId, Quay> quays = new HashMap<>();
  private final Map<QuayId, StopPlace> stopPlaceForQuay = new HashMap<>();
  private final Map<QuayId, QuayCoordinates> coordinatesForQuay =
    new HashMap<>();
  private final Map<QuayId, TransportModeAndSubMode> transportModesForQuay =
    new HashMap<>();
  private final Map<QuayId, String> stopPlaceNameForQuay = new HashMap<>();

  public TestStopPlaceRepository() {}

  TestStopPlaceRepository(Map<StopPlace, Quay> quayForStopPlace) {
    quayForStopPlace.forEach((stopPlace, quay) -> {
      QuayId quayId = QuayId.ofValidId(quay);
      stopPlaces.put(new StopPlaceId(stopPlace.getId()), stopPlace);
      quays.put(quayId, quay);
      stopPlaceForQuay.put(quayId, stopPlace);
    });
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * bus/local bus
   */
  public static TestStopPlaceRepository ofLocalBusStops(int numStops) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * bus/rail replacement bus
   */
  public static TestStopPlaceRepository ofRailReplacementBusStops(
    int numStops
  ) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
          .withBusSubmode(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * coach/national coach
   */
  public static TestStopPlaceRepository ofNationalCoachStops(int numStops) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllVehicleModesOfTransportEnumeration.COACH)
          .withCoachSubmode(CoachSubmodeEnumeration.NATIONAL_COACH)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * rail/local
   */
  public static TestStopPlaceRepository ofLocalTrainStops(int numStops) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllVehicleModesOfTransportEnumeration.RAIL)
          .withRailSubmode(RailSubmodeEnumeration.LOCAL)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays where the transport modes and
   * submodes are missing
   */
  public static TestStopPlaceRepository ofMissingTransportModeAndSubMode(
    int numStops
  ) {
    return ofTransportMode(numStops, Function.identity());
  }

  private static TestStopPlaceRepository ofTransportMode(
    int numStops,
    Function<StopPlace, StopPlace> setTransportMode
  ) {
    Map<StopPlace, Quay> stopPlaceQuayMap = new HashMap<>();
    IntStream
      .rangeClosed(1, numStops)
      .forEach(index ->
        stopPlaceQuayMap.put(
          setTransportMode.apply(
            new StopPlace().withId("TST:StopPlace:" + index)
          ),
          new Quay().withId("TST:Quay:" + index)
        )
      );
    return new TestStopPlaceRepository(stopPlaceQuayMap);
  }

  /**
   * Register a quay under a stop place id, without a transport mode or coordinates.
   */
  public TestStopPlaceRepository putQuay(
    StopPlaceId stopPlaceId,
    QuayId quayId
  ) {
    StopPlace stopPlace = new StopPlace().withId(stopPlaceId.id());
    stopPlaces.put(stopPlaceId, stopPlace);
    quays.put(quayId, new Quay().withId(quayId.id()));
    stopPlaceForQuay.put(quayId, stopPlace);
    return this;
  }

  public TestStopPlaceRepository putCoordinates(
    QuayId quayId,
    QuayCoordinates quayCoordinates
  ) {
    coordinatesForQuay.put(quayId, quayCoordinates);
    return this;
  }

  public TestStopPlaceRepository putTransportModes(
    QuayId quayId,
    TransportModeAndSubMode transportModeAndSubMode
  ) {
    transportModesForQuay.put(quayId, transportModeAndSubMode);
    return this;
  }

  public TestStopPlaceRepository putStopPlaceName(
    QuayId quayId,
    String stopPlaceName
  ) {
    stopPlaceNameForQuay.put(quayId, stopPlaceName);
    return this;
  }

  @Override
  public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
    return stopPlaces.containsKey(stopPlaceId);
  }

  @Override
  public boolean hasQuayId(QuayId quayId) {
    return quays.containsKey(quayId);
  }

  @Nullable
  @Override
  public TransportModeAndSubMode getTransportModesForQuayId(QuayId quayId) {
    TransportModeAndSubMode explicit = transportModesForQuay.get(quayId);
    if (explicit != null) {
      return explicit;
    }
    return TransportModeAndSubMode.of(stopPlaceForQuay.get(quayId));
  }

  @Nullable
  @Override
  public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
    QuayCoordinates explicit = coordinatesForQuay.get(quayId);
    if (explicit != null) {
      return explicit;
    }
    return QuayCoordinates.of(quays.get(quayId));
  }

  @Nullable
  @Override
  public String getStopPlaceNameForQuayId(QuayId quayId) {
    String explicit = stopPlaceNameForQuay.get(quayId);
    if (explicit != null) {
      return explicit;
    }
    StopPlace stopPlace = stopPlaceForQuay.get(quayId);
    if (stopPlace == null || stopPlace.getName() == null) {
      return null;
    }
    return stopPlace.getName().getValue();
  }
}
