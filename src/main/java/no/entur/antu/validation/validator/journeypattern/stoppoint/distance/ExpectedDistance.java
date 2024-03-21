package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

/**
 * Expected distance between two consecutive stop points in journey pattern for different modes of transport.
 */
public record ExpectedDistance(long maxDistance, long minDistance) {
  private static ExpectedDistance of(
    /*
     * The maximum distance between two consecutive stop points in journey pattern for the mode of transport.
     */
    long maxDistance,
    /*
     * The minimum distance between two consecutive stop points in journey pattern for the mode of transport.
     */
    long minDistance
  ) {
    return new ExpectedDistance(maxDistance, minDistance);
  }

  public static ExpectedDistance of(
    AllVehicleModesOfTransportEnumeration transportMode
  ) {
    return switch (transportMode) {
      case COACH -> of(30000, 500);
      case AIR -> of(2100000, 30000);
      case BUS -> of(10000, 100);
      case METRO -> of(2000, 200);
      case RAIL -> of(300000, 1000);
      case TRAM -> of(1000, 200);
      case FUNICULAR -> of(1000, 100);
      case TAXI -> of(300000, 500);
      case OTHER -> of(30000, 300);
      case WATER, FERRY -> of(30000, 200);
      case TROLLEY_BUS, CABLEWAY, LIFT -> of(2000, 300);
      default -> of(200, 20);
    };
  }
}
