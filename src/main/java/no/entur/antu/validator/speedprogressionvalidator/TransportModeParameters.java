package no.entur.antu.validator.speedprogressionvalidator;

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

public record TransportModeParameters(long maxSpeed, long warningSpeed, long minSpeed) {

    private static TransportModeParameters of(long maxSpeed, long warningSpeed, long minSpeed) {
        return new TransportModeParameters(maxSpeed, warningSpeed, minSpeed);
    }

    public static TransportModeParameters of(AllVehicleModesOfTransportEnumeration transportMode) {
        return switch (transportMode) {
            case COACH -> of(130, 80, 10);
            case AIR -> of(1000, 900, 10);
            case BUS -> of(120, 70, 5);
            case METRO -> of(70, 50 , 15);
            case RAIL -> of(210, 180, 10);
            case TROLLEY_BUS, TRAM -> of(70, 50, 10);
            case WATER -> of(200, 50,  5);
            case CABLEWAY, FUNICULAR -> of(80, 30, 10);
            case TAXI -> of(130, 110, 1);
            case INTERCITY_RAIL, URBAN_RAIL, ALL, UNKNOWN, SNOW_AND_ICE, SELF_DRIVE -> null;
        };
    }


    /*
            //case OTHER -> of(50, 40, 1);
            //case LIFT -> of(60, 50, 20);
            //case FERRY -> of(200, 50 , 5);
    "mode_walk": {
        "speed_max": "10",
        "speed_warning": "6",
        "speed_min": "1",
    },
    "mode_private_vehicle": {
        "speed_max": "130",
        "speed_warning": "110",
        "speed_min": "2",
    },
    "mode_bicycle": {
        "speed_max": "40",
        "speed_warning": "30",
        "speed_min": "1",
    },
    */

}
