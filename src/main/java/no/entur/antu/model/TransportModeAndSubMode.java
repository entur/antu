package no.entur.antu.model;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.StopPlace;

public record TransportModeAndSubMode(
  AllVehicleModesOfTransportEnumeration mode,
  TransportSubMode subMode
) {
  public static TransportModeAndSubMode of(StopPlace stopPlace) {
    return new TransportModeAndSubMode(
      stopPlace.getTransportMode(),
      TransportSubMode.of(stopPlace).orElse(null)
    );
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return (
      (mode != null ? mode.value() : "") +
      (subMode != null ? "§" + subMode.name() : "")
    );
  }

  /*
   * Used to decode data stored in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  public static TransportModeAndSubMode fromString(
    String stopPlaceTransportModeAndSubMode
  ) {
    if (stopPlaceTransportModeAndSubMode != null) {
      String[] split = stopPlaceTransportModeAndSubMode.split("§");
      if (split.length == 1) {
        return new TransportModeAndSubMode(
          AllVehicleModesOfTransportEnumeration.fromValue(split[0]),
          null
        );
      } else if (split.length == 2) {
        return new TransportModeAndSubMode(
          AllVehicleModesOfTransportEnumeration.fromValue(split[0]),
          new TransportSubMode(split[1])
        );
      } else {
        throw new AntuException(
          "Invalid stopPlaceTransportModeAndSubMode string: " +
          stopPlaceTransportModeAndSubMode
        );
      }
    }
    return null;
  }
}
