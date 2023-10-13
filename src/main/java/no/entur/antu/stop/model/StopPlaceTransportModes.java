package no.entur.antu.stop.model;

import org.rutebanken.netex.model.VehicleModeEnumeration;

public record StopPlaceTransportModes(
        VehicleModeEnumeration mode,
        TransportSubMode subMode) {
}