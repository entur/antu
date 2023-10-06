package no.entur.antu.stop.model;

import org.rutebanken.netex.model.VehicleModeEnumeration;

public record TransportModes(VehicleModeEnumeration mode, TransportSubMode subMode) {
}
