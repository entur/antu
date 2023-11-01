package no.entur.antu.stop.model;

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;


public record StopPlaceTransportModes(
        AllVehicleModesOfTransportEnumeration mode,
        TransportSubMode subMode) {
}