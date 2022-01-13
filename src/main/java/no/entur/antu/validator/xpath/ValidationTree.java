package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.xml.XMLParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ValidationTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationTree.class);

    private final String name;
    private final String context;
    private final List<ValidationTree> subTrees;
    private final List<ValidationRule> validationRules;
    private final Predicate<XPathValidationContext> executionCondition;

    public ValidationTree(String name, String context) {
        this(name, context, validationContext -> true);
    }

    public ValidationTree(String name, String context, Predicate<XPathValidationContext> executionCondition) {
        this.name = name;
        this.context = context;
        this.executionCondition = executionCondition;
        this.validationRules = new ArrayList<>();
        this.subTrees = new ArrayList<>();
    }

    public List<ValidationReportEntry> validate(XPathValidationContext validationContext) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        for (ValidationRule validationRule : validationRules) {
            LOGGER.debug("Running validation rule '{}'/'{}'", name, validationRule.getMessage());
            validationReportEntries.addAll(validationRule.validate(validationContext));
        }
        for (ValidationTree validationSubTree : subTrees) {
            XdmValue subContextNodes = XMLParserUtil.selectNodeSet(validationSubTree.getContext(), validationContext.getXPathCompiler(), validationContext.getXmlNode());
            for (XdmItem xdmItem : subContextNodes) {
                XPathValidationContext validationSubContext = new XPathValidationContext((XdmNode) xdmItem, validationContext.getXPathCompiler(), validationContext.getCodespace(), validationContext.getFileName());
                if (validationSubTree.executionCondition.test(validationSubContext)) {
                    LOGGER.debug("Running validation subtree '{}'/'{}'", name, validationSubTree.getName());
                    validationReportEntries.addAll(validationSubTree.validate(validationSubContext));
                } else {
                    LOGGER.debug("Skipping validation subtree '{}'/'{}'", name, validationSubTree.getName());
                }
            }

        }

        return validationReportEntries;


    }

    public String describe() {
        return describe(0);
    }

    private String describe(int indentation) {
        StringBuilder builder = new StringBuilder();
        char[] spaces = new char[indentation];
        Arrays.fill(spaces, ' ');
        for (ValidationRule validationRule : validationRules) {
            builder.append(spaces)
                    .append("[")
                    .append(validationRule.getName())
                    .append("] ")
                    .append("[")
                    .append(validationRule.getSeverity())
                    .append("] ")
                    .append(validationRule.getMessage())
                    .append("\n");
        }
        for (ValidationTree validationTree : subTrees) {
            builder.append(validationTree.describe(indentation + 2));
        }
        return builder.toString();
    }

    public Set<String> getRuleMessages() {
        Set<String> rules = new HashSet<>();
        for (ValidationRule validationRule : validationRules) {
            rules.add("[" + validationRule.getName() + "] " + validationRule.getMessage());
        }
        for (ValidationTree validationTree : subTrees) {
            rules.addAll(validationTree.getRuleMessages());
        }
        return rules;
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

    public String getName() {
        return name;
    }

}
