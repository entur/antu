package no.entur.antu.validator.xpath;

import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.rutebanken.netex.model.BookingMethodEnumeration;

public class ValidateAllowedBookingAccessProperty extends ValidateNotExist {

    private static final String VALID_BOOKING_ACCESS_PROPERTIES = "'" + String.join("','",
            BookingMethodEnumeration.CALL_DRIVER.value(),
            BookingMethodEnumeration.CALL_OFFICE.value(),
            BookingMethodEnumeration.ONLINE.value(),
            BookingMethodEnumeration.OTHER.value(),
            BookingMethodEnumeration.PHONE_AT_STOP.value(),
            BookingMethodEnumeration.TEXT.value())
            + "'";

    private static final String MESSAGE = "Illegal BookingAccess";

    public ValidateAllowedBookingAccessProperty(String context) {
        super(context + "/BookingAccess[not(. = (" + VALID_BOOKING_ACCESS_PROPERTIES + "))]", MESSAGE, "Booking", ValidationReportEntrySeverity.ERROR);
    }
}
