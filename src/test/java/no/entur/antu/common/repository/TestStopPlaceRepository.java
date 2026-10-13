package no.entur.antu.common.repository;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.model.MultilingualStringValue;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.entur.netex2.model.AllPublicTransportModesEnumeration;
import org.entur.netex2.model.BusSubmodeEnumeration;
import org.entur.netex2.model.CoachSubmodeEnumeration;
import org.entur.netex2.model.Quay;
import org.entur.netex2.model.RailSubmodeEnumeration;
import org.entur.netex2.model.StopPlace;
import org.jetbrains.annotations.Nullable;

public class TestStopPlaceRepository implements StopPlaceRepository {

  private final Map<StopPlaceId, StopPlace> stopPlaces;
  private final Map<QuayId, Quay> quays;
  private final Map<QuayId, StopPlace> stopPlaceForQuay;

  TestStopPlaceRepository(Map<StopPlace, Quay> quayforStopPlace) {
    this.quays =
      quayforStopPlace
        .values()
        .stream()
        .collect(
          Collectors.toUnmodifiableMap(QuayId::ofValidId, Function.identity())
        );
    this.stopPlaces =
      quayforStopPlace
        .keySet()
        .stream()
        .collect(
          Collectors.toUnmodifiableMap(
            stopPlace -> new StopPlaceId(stopPlace.getId()),
            Function.identity()
          )
        );
    this.stopPlaceForQuay =
      quayforStopPlace
        .entrySet()
        .stream()
        .collect(
          Collectors.toUnmodifiableMap(
            entry -> QuayId.ofValidId(entry.getValue()),
            Map.Entry::getKey
          )
        );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * bus/local bus
   */
  public static StopPlaceRepository ofLocalBusStops(int numStops) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllPublicTransportModesEnumeration.BUS)
          .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * bus/rail replacement bus
   */
  public static StopPlaceRepository ofRailReplacementBusStops(int numStops) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllPublicTransportModesEnumeration.BUS)
          .withBusSubmode(BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * coach/national coach
   */

  public static StopPlaceRepository ofNationalCoachStops(int numStops) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllPublicTransportModesEnumeration.COACH)
          .withCoachSubmode(CoachSubmodeEnumeration.NATIONAL_COACH)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays with transport mode/submode
   * rail/local
   */
  public static StopPlaceRepository ofLocalTrainStops(int numStops) {
    return ofTransportMode(
      numStops,
      stopPlace ->
        stopPlace
          .withTransportMode(AllPublicTransportModesEnumeration.RAIL)
          .withRailSubmode(RailSubmodeEnumeration.LOCAL)
    );
  }

  /**
   * Return a stop place repository containing numStops stop places and numStops quays where the transport modes and
   * submodes are missing
   */
  public static StopPlaceRepository ofMissingTransportModeAndSubMode(
    int numStops
  ) {
    return ofTransportMode(numStops, Function.identity());
  }

  private static StopPlaceRepository ofTransportMode(
    int numStops,
    Function<StopPlace, StopPlace> setTransportMode
  ) {
    Map<StopPlace, Quay> stopPlaceQuayMap = IntStream
      .rangeClosed(1, numStops)
      .boxed()
      .collect(
        Collectors.toUnmodifiableMap(
          stopIndex ->
            setTransportMode.apply(
              new StopPlace().withId("TST:StopPlace:" + stopIndex)
            ),
          quayIndex -> new Quay().withId("TST:Quay:" + quayIndex)
        )
      );
    return new TestStopPlaceRepository(stopPlaceQuayMap);
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
    return TransportModeAndSubMode.of(stopPlaceForQuay.get(quayId));
  }

  @Nullable
  @Override
  public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
    return QuayCoordinates.of(quays.get(quayId));
  }

  @Nullable
  @Override
  public String getStopPlaceNameForQuayId(QuayId quayId) {
    return MultilingualStringValue.of(stopPlaceForQuay.get(quayId).getName());
  }
}
