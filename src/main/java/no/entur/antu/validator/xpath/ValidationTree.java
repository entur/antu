package no.entur.antu.validator.xpath;

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
import java.util.function.Predicate;

public class ValidationTree {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationTree.class);

    private final String name;
    private final String context;
    private final Set<ValidationTree> subTrees;
    private final Set<ValidationRule> validationRules;
    private final Predicate<ValidationContext> executionCondition;

    public ValidationTree(String name, String context) {
        this(name, context, validationContext -> true);
    }

    public ValidationTree(String name, String context, Predicate<ValidationContext> executionCondition) {
        this.name = name;
        this.context = context;
        this.executionCondition = executionCondition;
        this.validationRules = new HashSet<>();
        this.subTrees = new HashSet<>();
    }

    public List<ValidationReportEntry> validate(ValidationContext validationContext) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        for (ValidationRule validationRule : validationRules) {
            LOGGER.debug("Running validation rule '{}'/'{}'", name, validationRule.getMessage());
            validationReportEntries.addAll(validationRule.validate(validationContext));
        }
        for (ValidationTree validationSubTree : subTrees) {
            XdmValue subContextNodes = XMLParserUtil.selectNodeSet(validationSubTree.getContext(), validationContext.getxPathCompiler(), validationContext.getXmlNode());
            for (XdmItem xdmItem : subContextNodes) {
                ValidationContext validationSubContext = new ValidationContext((XdmNode) xdmItem, validationContext.getxPathCompiler(), validationContext.getCodespace(), validationContext.getFileName());
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
