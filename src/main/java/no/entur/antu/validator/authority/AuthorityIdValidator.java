package no.entur.antu.validator.authority;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.xml.XMLParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Validate that NeTEx Authority identifier are valid according to the Organisation register.
 */
public class AuthorityIdValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityIdValidator.class);

    private final OrganisationRepository organisationRepository;

    public AuthorityIdValidator(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    public List<ValidationReportEntry> validateAuthorityId(String codespace, String fileName, byte[] content) throws XMLStreamException, SaxonApiException {
        Set<String> whitelistedAuthorityIds = organisationRepository.getWhitelistedAuthorityIds(codespace);
        if (whitelistedAuthorityIds.isEmpty()) {
            return Collections.emptyList();
        } else {
            XdmNode document = XMLParserUtil.parseFileToXdmNode(content);
            String xpath = "//ResourceFrame/organisations/Authority[not(@id=('" + String.join("','", whitelistedAuthorityIds) + "'))]";
            XPathSelector selector = XMLParserUtil.getXPathCompiler().compile(xpath).load();
            selector.setContextItem(document);
            XdmValue nodes = selector.evaluate();
            List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
            for (XdmItem item : nodes) {
                XdmNode xdmNode = (XdmNode) item;

                int lineNumber = xdmNode.getLineNumber();
                int columnNumber = xdmNode.getColumnNumber();
                String netexId = xdmNode.getAttributeValue(new QName("id"));

                String message = "Line " + lineNumber + ", Column " + columnNumber + ": Invalid Authority ID " + netexId;
                LOGGER.warn(message);
                validationReportEntries.add(new ValidationReportEntry(message, "Invalid Authority Id", ValidationReportEntrySeverity.WARNING, fileName));
            }
            return validationReportEntries;
        }
    }

}
