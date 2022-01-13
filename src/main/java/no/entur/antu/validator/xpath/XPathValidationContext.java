package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;

public class XPathValidationContext {

    private final XdmNode xmlNode;
    private final XPathCompiler xPathCompiler;

    private final String codespace;
    private final String fileName;

    public XPathValidationContext(XdmNode document, XPathCompiler xPathCompiler, String codespace, String fileName) {
        this.xmlNode = document;
        this.xPathCompiler = xPathCompiler;
        this.codespace = codespace;
        this.fileName = fileName;
    }

    public XdmNode getXmlNode() {
        return xmlNode;
    }

    public XPathCompiler getxPathCompiler() {
        return xPathCompiler;
    }

    public String getFileName() {
        return fileName;
    }


    public String getCodespace() {
        return codespace;
    }
}
