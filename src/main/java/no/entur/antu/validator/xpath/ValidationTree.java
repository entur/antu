package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.xml.XMLParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ValidationTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationTree.class);

    private final String context;
    private final Set<ValidationTree> subTrees;
    private final Set<ValidationRule> validationRules;

    public ValidationTree(String context) {
        this.context = context;
        this.validationRules = new HashSet<>();
        this.subTrees = new HashSet<>();
    }

    public List<ValidationReportEntry> validate(ValidationContext validationContext) throws SaxonApiException {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        for (ValidationRule validationRule : validationRules) {
            LOGGER.debug("Running validation rule: {}", validationRule.getMessage());
            validationReportEntries.addAll(validationRule.validate(validationContext));
        }
        for (ValidationTree validationTree : subTrees) {
            LOGGER.debug("Running validation sub tree: {}", validationTree.getContext());
            XdmValue subContextNodes = XMLParserUtil.selectNodeSet(validationTree.getContext(), validationContext.getxPathCompiler(), validationContext.getXmlNode());
            for (XdmItem xdmItem : subContextNodes) {
                ValidationContext validationSubContext = new ValidationContext((XdmNode) xdmItem, validationContext.getxPathCompiler(), validationContext.getCodespace(), validationContext.getFileName());
                validationReportEntries.addAll(validationTree.validate(validationSubContext));
            }
        }
        return validationReportEntries;


    }

    public void addValidationRule(ValidationRule validationRule) {
        validationRules.add(validationRule);
    }

    public void addValidationRules(List<ValidationRule> validationRules) {
        this.validationRules.addAll(validationRules);
    }

    public void addSubTree(ValidationTree validationTree) {
        subTrees.add(validationTree);
    }


    public String getContext() {
        return context;
    }


}
