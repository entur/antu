package no.entur.antu.validator.xpath.rules;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.exception.AntuException;
import no.entur.antu.organisation.OrganisationRepository;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Validate the Authority ids against the Organisation Register.
 */
public class ValidateAuthorityId extends AbstractXPathValidationRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateAuthorityId.class);

    private static final String MESSAGE = "Invalid Authority Id";
    private static final String RULE_CODE = "AUTHORITY_ID";

    private final OrganisationRepository organisationRepository;

    public ValidateAuthorityId(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    @Override
    public List<XPathValidationReportEntry> validate(XPathValidationContext validationContext) {
        try {
            Set<String> whitelistedAuthorityIds = organisationRepository.getWhitelistedAuthorityIds(validationContext.getCodespace());
            if (whitelistedAuthorityIds.isEmpty()) {
                return Collections.emptyList();
            } else {
                String xpath = "//ResourceFrame/organisations/Authority[not(@id=('" + String.join("','", whitelistedAuthorityIds) + "'))]";
                XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler().compile(xpath).load();
                selector.setContextItem(validationContext.getXmlNode());
                XdmValue nodes = selector.evaluate();
                List<XPathValidationReportEntry> validationReportEntries = new ArrayList<>();
                for (XdmItem item : nodes) {
                    XdmNode xdmNode = (XdmNode) item;
                    DataLocation dataLocation = getXdmNodeLocation(validationContext.getFileName(), xdmNode);
                    LOGGER.warn("{}" + MESSAGE, dataLocation);
                    validationReportEntries.add(new XPathValidationReportEntry(MESSAGE, RULE_CODE, dataLocation));
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

    @Override
    public String getCode() {
        return RULE_CODE;
    }

}
