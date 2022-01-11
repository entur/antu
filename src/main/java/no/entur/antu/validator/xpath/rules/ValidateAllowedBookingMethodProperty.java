package no.entur.antu.validator.xpath.rules;

import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.rutebanken.netex.model.BookingMethodEnumeration;

public class ValidateAllowedBookingMethodProperty extends ValidateNotExist {

    private static final String VALID_BOOKING_METHOD_PROPERTIES = "'" + String.join("','",
            BookingMethodEnumeration.CALL_DRIVER.value(),
            BookingMethodEnumeration.CALL_OFFICE.value(),
            BookingMethodEnumeration.ONLINE.value(),
            BookingMethodEnumeration.OTHER.value(),
            BookingMethodEnumeration.PHONE_AT_STOP.value(),
            BookingMethodEnumeration.TEXT.value())
            + "'";

    private static final String MESSAGE = "Illegal value for BookingMethod";

    public ValidateAllowedBookingMethodProperty(String context) {
        super(context + "/BookingMethods[tokenize(.,' ')[not(. = (" + VALID_BOOKING_METHOD_PROPERTIES + "))]]", MESSAGE, "BOOKING_2", ValidationReportEntrySeverity.ERROR);
    }
}