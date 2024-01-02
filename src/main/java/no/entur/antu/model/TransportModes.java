package no.entur.antu.model;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

public record TransportModes(
  AllVehicleModesOfTransportEnumeration mode,
  TransportSubMode subMode
) {
  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return mode.value() + (subMode != null ? "§" + subMode.name() : "");
  }

  /*
   * Used to decode data stored in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  public static TransportModes fromString(String stopPlaceTransportModes) {
    if (stopPlaceTransportModes != null) {
      String[] split = stopPlaceTransportModes.split("§");
      if (split.length == 1) {
        return new TransportModes(
          AllVehicleModesOfTransportEnumeration.fromValue(split[0]),
          null
        );
      } else if (split.length == 2) {
        return new TransportModes(
          AllVehicleModesOfTransportEnumeration.fromValue(split[0]),
          new TransportSubMode(split[1])
        );
      } else {
        throw new AntuException(
          "Invalid stopPlaceTransportModes string: " + stopPlaceTransportModes
        );
      }
    }
    return null;
  }
}
