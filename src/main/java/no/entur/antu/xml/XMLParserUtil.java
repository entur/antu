package no.entur.antu.xml;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.WhitespaceStrippingPolicy;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import java.io.ByteArrayInputStream;

public final class XMLParserUtil {

    private static final String NETEX_NAMESPACE = "http://www.netex.org.uk/netex";
    private static final String SIRI_NAMESPACE = "http://www.siri.org.uk/siri";
    private static final String OPENGIS_NAMESPACE = "http://www.opengis.net/gml/3.2";

    private static XPathCompiler xpathCompiler;
    private static final Processor processor = new Processor(false);

    private XMLParserUtil() {
    }

    public static XMLInputFactory getSecureXmlInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        return factory;
    }

    public static Processor getProcessor() {
        return processor;
    }

    public static synchronized XPathCompiler getXPathCompiler() {

        if (xpathCompiler == null) {
            xpathCompiler = processor.newXPathCompiler();
            xpathCompiler.setCaching(true);
            xpathCompiler.declareNamespace("", NETEX_NAMESPACE);
            xpathCompiler.declareNamespace("n", NETEX_NAMESPACE);
            xpathCompiler.declareNamespace("s", SIRI_NAMESPACE);
            xpathCompiler.declareNamespace("g", OPENGIS_NAMESPACE);
        }

        return xpathCompiler;
    }

    public static XdmNode parseFileToXdmNode(byte[] content) throws SaxonApiException, XMLStreamException {
        DocumentBuilder builder = getProcessor().newDocumentBuilder();
        builder.setLineNumbering(true);
        builder.setWhitespaceStrippingPolicy(WhitespaceStrippingPolicy.ALL);
        XMLInputFactory factory = getSecureXmlInputFactory();
        XMLStreamReader xmlStreamReader = factory.createXMLStreamReader(new ByteArrayInputStream(content));
        return builder.build(new StAXSource(xmlStreamReader));
    }

    public static XdmValue selectNodeSet(String expression, XPathCompiler xpath, XdmNode document) throws SaxonApiException {
        XPathSelector selector = xpath.compile(expression).load();
        selector.setContextItem(document);
        return selector.evaluate();
    }

}
