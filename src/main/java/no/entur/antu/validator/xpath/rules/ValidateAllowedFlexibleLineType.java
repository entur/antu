package no.entur.antu.validator.xpath.rules;

import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;

public class ValidateAllowedFlexibleLineType extends ValidateNotExist {

    private static final String VALID_FLEXIBLE_LINE_TYPES = "'" + String.join("','",
            FlexibleLineTypeEnumeration.CORRIDOR_SERVICE.value(),
            FlexibleLineTypeEnumeration.MAIN_ROUTE_WITH_FLEXIBLE_ENDS.value(),
            FlexibleLineTypeEnumeration.FLEXIBLE_AREAS_ONLY.value(),
            FlexibleLineTypeEnumeration.HAIL_AND_RIDE_SECTIONS.value(),
            FlexibleLineTypeEnumeration.FIXED_STOP_AREA_WIDE.value(),
            FlexibleLineTypeEnumeration.MIXED_FLEXIBLE.value(),
            FlexibleLineTypeEnumeration.MIXED_FLEXIBLE_AND_FIXED.value(),
            FlexibleLineTypeEnumeration.FIXED.value())
            + "'";

    public static final String MESSAGE = "Illegal FlexibleLineType on FlexibleLine";

    public ValidateAllowedFlexibleLineType() {
        super("lines/FlexibleLine/FlexibleLineType[not(. = (" + VALID_FLEXIBLE_LINE_TYPES + "))]", MESSAGE, "Flexible Lines", ValidationReportEntrySeverity.ERROR);
    }
}

