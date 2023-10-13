package no.entur.antu.validator.transportmodevalidator;

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;

public record DatasetTransportModes(
        AllVehicleModesOfTransportEnumeration mode,
        BusSubmodeEnumeration busSubMode) {
}
