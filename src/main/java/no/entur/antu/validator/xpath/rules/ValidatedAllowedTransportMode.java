package no.entur.antu.validator.xpath.rules;

import no.entur.antu.validator.ValidationReportEntrySeverity;

public class ValidatedAllowedTransportMode extends ValidateNotExist {

    private static final String VALID_TRANSPORT_MODES = "'" + String.join("','",
            "coach",
            "bus",
            "tram",
            "rail",
            "metro",
            "air",
            "water",
            "cableway",
            "funicular",
            "unknown")
            + "'";

    private static final String MESSAGE = "Illegal TransportMode";

    public ValidatedAllowedTransportMode() {
        super("lines/*[self::Line or self::FlexibleLine]/TransportMode[not(. = (" + VALID_TRANSPORT_MODES + "))]", MESSAGE, "Service Frame", ValidationReportEntrySeverity.ERROR);
    }
}
