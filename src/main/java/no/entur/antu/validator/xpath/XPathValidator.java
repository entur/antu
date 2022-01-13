package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.validator.NetexValidator;
import no.entur.antu.validator.ValidationReport;
import no.entur.antu.validator.ValidationReportEntry;

import java.util.List;
import java.util.Set;

/**
 * Run XPath validation rules against the dataset.
 */
public class XPathValidator implements NetexValidator {

    private final ValidationTree topLevelValidationTree;

    public XPathValidator(ValidationTreeFactory validationTreeFactory) {
        this.topLevelValidationTree = validationTreeFactory.buildValidationTree();
    }

    @Override
    public void validate(ValidationReport validationReport, ValidationContext validationContext) {
        List<ValidationReportEntry> validationReportEntries = validate(validationReport.getCodespace(), validationContext.getFileName(), validationContext.getXmlNode(), validationContext.getxPathCompiler());
        validationReport.addAllValidationReportEntries(validationReportEntries);
    }

    protected List<ValidationReportEntry> validate(String codespace, String fileName, XdmNode document, XPathCompiler xPathCompiler) {
        XPathValidationContext validationContext = new XPathValidationContext(document, xPathCompiler, codespace, fileName);
        return this.validate(validationContext);

    }

    public List<ValidationReportEntry> validate(XPathValidationContext validationContext) {
        return topLevelValidationTree.validate(validationContext);
    }

    public String describe() {
        return topLevelValidationTree.describe();
    }

    public Set<String> getRuleMessages() {
        return topLevelValidationTree.getRuleMessages();
    }


}
