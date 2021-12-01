package no.entur.antu.validator.xpath;

import no.entur.antu.validator.ValidationReportEntrySeverity;

public class ValidateNSRCodespaceExist extends ValidateExist {

    private static final String NSR_XMLNSURL = "http://www.rutebanken.org/ns/nsr";
    private static final String NSR_XMLNS = "NSR";

    public ValidateNSRCodespaceExist() {
        super("codespaces/Codespace[Xmlns = '" + NSR_XMLNS + "' and XmlnsUrl = '" + NSR_XMLNSURL + "']",
                "NSR Codespace must be declared (Xmlns=NSR, XmlnsUrl=http://www.rutebanken.org/ns/nsr). Any references to StopPlaces must point to data in the NSR namespace",
                "Composite Frame",
                ValidationReportEntrySeverity.ERROR);
    }
}
