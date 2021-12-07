package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.ArrayList;
import java.util.List;

public class ValidateMandatoryBookingProperty implements ValidationRule {

    private static final String MESSAGE = "[Line %s, Column %s, Id %s] Mandatory booking property %s not specified on FlexibleServiceProperties, FlexibleLine or on all StopPointInJourneyPatterns";

    private final String bookingProperty;

    public ValidateMandatoryBookingProperty(String bookingProperty) {
        this.bookingProperty = bookingProperty;
    }

    @Override
    public List<ValidationReportEntry> validate(ValidationContext validationContext) {
        try {
            List<XdmValue> errorNodes = new ArrayList<>();
            XPathSelector missingFieldSelector = validationContext.getxPathCompiler().compile("lines/FlexibleLine and lines/FlexibleLine[not(" + bookingProperty + ")]").load();
            missingFieldSelector.setContextItem(validationContext.getXmlNode());
            boolean missingField = missingFieldSelector.effectiveBooleanValue();
            if (missingField) {
                XPathSelector selector = validationContext.getxPathCompiler().compile("journeyPatterns/*[self::JourneyPattern][pointsInSequence/StopPointInJourneyPattern[not(BookingArrangements/" + bookingProperty + ")]]").load();
                selector.setContextItem(validationContext.getXmlNode());
                XdmValue nodes = selector.evaluate();

                for (XdmValue value : nodes) {
                    if (value instanceof XdmNode) {
                        XdmNode node = (XdmNode) value;
                        String id = node.getAttributeValue(QName.fromEQName("id"));
                        String version = node.getAttributeValue(QName.fromEQName("version"));

                        XPathSelector sjSelector = validationContext.getxPathCompiler().compile("//vehicleJourneys/ServiceJourney[(not(FlexibleServiceProperties) or not(FlexibleServiceProperties/" + bookingProperty + ")) and JourneyPatternRef/@ref='" + id + "' and @version='" + version + "']").load();
                        sjSelector.setContextItem(validationContext.getXmlNode());
                        XdmValue errorsForJP = sjSelector.evaluate();
                        if (errorsForJP.size() > 0) {
                            errorNodes.add(errorsForJP);
                        }
                    }
                }
            }
            List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

            for (XdmValue errorNode : errorNodes) {
                for (XdmItem item : errorNode) {
                    XdmNode xdmNode = (XdmNode) item;
                    String formattedMessage = String.format(MESSAGE,
                            xdmNode.getLineNumber(),
                            xdmNode.getColumnNumber(),
                            xdmNode.getAttributeValue(new QName("id")),
                            bookingProperty);
                    validationReportEntries.add(new ValidationReportEntry(formattedMessage,
                            "Flexible Line",
                            ValidationReportEntrySeverity.ERROR,
                            validationContext.getFileName()));
                }


            }
            return validationReportEntries;
        } catch (SaxonApiException e) {
            throw new AntuException("Error while validating rule", e);
        }
    }

    @Override
    public String getMessage() {
        return MESSAGE;
    }
}
