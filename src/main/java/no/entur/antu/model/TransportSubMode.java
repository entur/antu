package no.entur.antu.model;

import java.util.Objects;
import java.util.Optional;
import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.StopPlace;

public record TransportSubMode(String name) {
  public TransportSubMode {
    Objects.requireNonNull(name, "Invalid transport sub mode " + name);
  }

  public static Optional<TransportSubMode> of(StopPlace stopPlace) {
    if (stopPlace == null || stopPlace.getTransportMode() == null) {
      return Optional.empty();
    }
    String subModeName =
      switch (stopPlace.getTransportMode()) {
        case AIR -> stopPlace.getAirSubmode() == null
          ? null
          : stopPlace.getAirSubmode().value();
        case BUS -> stopPlace.getBusSubmode() == null
          ? null
          : stopPlace.getBusSubmode().value();
        case COACH -> stopPlace.getCoachSubmode() == null
          ? null
          : stopPlace.getCoachSubmode().value();
        case METRO -> stopPlace.getMetroSubmode() == null
          ? null
          : stopPlace.getMetroSubmode().value();
        case RAIL -> stopPlace.getRailSubmode() == null
          ? null
          : stopPlace.getRailSubmode().value();
        case TRAM -> stopPlace.getTramSubmode() == null
          ? null
          : stopPlace.getTramSubmode().value();
        case WATER -> stopPlace.getWaterSubmode() == null
          ? null
          : stopPlace.getWaterSubmode().value();
        case CABLEWAY -> stopPlace.getTelecabinSubmode() == null
          ? null
          : stopPlace.getTelecabinSubmode().value();
        case FUNICULAR -> stopPlace.getFunicularSubmode() == null
          ? null
          : stopPlace.getFunicularSubmode().value();
        case SNOW_AND_ICE -> stopPlace.getSnowAndIceSubmode() == null
          ? null
          : stopPlace.getSnowAndIceSubmode().value();
        default -> throw new AntuException(
          "Unsupported Transport mode in stop place, while getting sub transport mode: " +
          stopPlace
        );
      };
    return Optional.ofNullable(
      subModeName == null ? null : new TransportSubMode(subModeName)
    );
  }
}
