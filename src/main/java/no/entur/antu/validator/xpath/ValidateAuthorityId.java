package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.exception.AntuException;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.xml.XMLParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ValidateAuthorityId implements ValidationRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateAuthorityId.class);
    private static final String MESSAGE = "Invalid Authority Id";

    private final OrganisationRepository organisationRepository;

    public ValidateAuthorityId(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    @Override
    public List<ValidationReportEntry> validate(ValidationContext validationContext) {
        try {
            Set<String> whitelistedAuthorityIds = organisationRepository.getWhitelistedAuthorityIds(validationContext.getCodespace());
            if (whitelistedAuthorityIds.isEmpty()) {
                return Collections.emptyList();
            } else {
                String xpath = "//ResourceFrame/organisations/Authority[not(@id=('" + String.join("','", whitelistedAuthorityIds) + "'))]";
                XPathSelector selector = XMLParserUtil.getXPathCompiler().compile(xpath).load();
                selector.setContextItem(validationContext.getXmlNode());
                XdmValue nodes = selector.evaluate();
                List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
                for (XdmItem item : nodes) {
                    XdmNode xdmNode = (XdmNode) item;

                    int lineNumber = xdmNode.getLineNumber();
                    int columnNumber = xdmNode.getColumnNumber();
                    String netexId = xdmNode.getAttributeValue(new QName("id"));

                    String message = "Line " + lineNumber + ", Column " + columnNumber + ", NeTEx id " + netexId + ": " +  MESSAGE ;
                    LOGGER.warn(message);
                    validationReportEntries.add(new ValidationReportEntry(message, MESSAGE, ValidationReportEntrySeverity.WARNING, validationContext.getFileName()));
                }
                return validationReportEntries;
            }
        } catch (SaxonApiException e) {
            throw new AntuException("Exception while validating authority ID", e);
        }

    }

    @Override
    public String getMessage() {
        return MESSAGE;
    }
}
