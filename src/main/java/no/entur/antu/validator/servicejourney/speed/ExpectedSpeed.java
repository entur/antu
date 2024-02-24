package no.entur.antu.validator.servicejourney.speed;

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

/**
 * Expected speed for different modes of transport.
 * The expected speed is used to validate the speed of a service journey.
 */
public record ExpectedSpeed(long maxSpeed, long warningSpeed, long minSpeed) {
  private static ExpectedSpeed of(
    /*
     * The maximum speed for the mode of transport.
     */
    long maxSpeed,
    /*
     * The speed that is considered a warning for the mode of transport.
     */
    long warningSpeed,
    /*
     * The minimum speed for the mode of transport.
     */
    long minSpeed
  ) {
    return new ExpectedSpeed(maxSpeed, warningSpeed, minSpeed);
  }

  public static ExpectedSpeed of(
    AllVehicleModesOfTransportEnumeration transportMode
  ) {
    return switch (transportMode) {
      case COACH -> of(130, 80, 10);
      case AIR -> of(1000, 900, 10);
      case BUS -> of(120, 70, 5);
      case METRO -> of(70, 50, 15);
      case RAIL -> of(210, 180, 10);
      case TROLLEY_BUS, TRAM -> of(70, 50, 10);
      case WATER -> of(200, 50, 5);
      case CABLEWAY, FUNICULAR -> of(80, 30, 10);
      case TAXI -> of(130, 110, 1);
      case LIFT -> of(60, 50, 20);
      case FERRY, OTHER -> of(50, 40, 1);
      case INTERCITY_RAIL,
        URBAN_RAIL,
        ALL,
        UNKNOWN,
        SNOW_AND_ICE,
        SELF_DRIVE,
        ANY_MODE -> null;
    };
  }
}
