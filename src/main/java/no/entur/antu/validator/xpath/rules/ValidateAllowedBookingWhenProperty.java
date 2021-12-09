package no.entur.antu.validator.xpath.rules;

import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.rutebanken.netex.model.PurchaseWhenEnumeration;

public class ValidateAllowedBookingWhenProperty extends ValidateNotExist {

    private static final String VALID_BOOKING_WHEN_PROPERTIES = "'" + String.join("','",
            PurchaseWhenEnumeration.TIME_OF_TRAVEL_ONLY.value(),
            PurchaseWhenEnumeration.DAY_OF_TRAVEL_ONLY.value(),
            PurchaseWhenEnumeration.UNTIL_PREVIOUS_DAY.value(),
            PurchaseWhenEnumeration.ADVANCE_ONLY.value(),
            PurchaseWhenEnumeration.ADVANCE_AND_DAY_OF_TRAVEL.value())
            + "'";

    private static final String MESSAGE = "Illegal value for BookWhen";

    public ValidateAllowedBookingWhenProperty(String context) {
        super(context + "/BookWhen[not(. = (" + VALID_BOOKING_WHEN_PROPERTIES + "))]", MESSAGE, "Booking", ValidationReportEntrySeverity.ERROR);
    }
}