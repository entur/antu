package no.entur.antu.validator.xpath;

import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.rutebanken.netex.model.FlexibleServiceEnumeration;

public class ValidateAllowedFlexibleServiceType extends ValidateNotExist {

    private static final String VALID_FLEXIBLE_SERVICE_TYPES = "'" + String.join("','",
            FlexibleServiceEnumeration.DYNAMIC_PASSING_TIMES.value(),
            FlexibleServiceEnumeration.FIXED_HEADWAY_FREQUENCY.value(),
            FlexibleServiceEnumeration.FIXED_PASSING_TIMES.value(),
            FlexibleServiceEnumeration.NOT_FLEXIBLE.value())
            + "'";

    public static final String MESSAGE = "Illegal FlexibleServiceType on ServiceJoruney";

    public ValidateAllowedFlexibleServiceType() {
        super("vehicleJourneys/ServiceJourney/FlexibleServiceProperties/FlexibleServiceType[not(. = (" + VALID_FLEXIBLE_SERVICE_TYPES + "))]", MESSAGE, "Service Frame", ValidationReportEntrySeverity.ERROR);
    }
}

