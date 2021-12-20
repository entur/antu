package no.entur.antu.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.namespace.QName;
import java.util.Set;

public class SkippingElementContentHandler extends DefaultHandler {

    private final XMLReader xmlReader;

    private final Set<QName> elementsToSkip;

    public SkippingElementContentHandler(XMLReader xmlReader, Set<QName> elementsToSkip) {
        this.xmlReader = xmlReader;
        this.elementsToSkip = elementsToSkip;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {

        if (skipElement(uri, localName)) {
            xmlReader.setContentHandler(new IgnoringContentHandler(xmlReader,
                    this));
        } else {
            super.startElement(uri, localName, qName, attributes);
        }
    }

    private boolean skipElement(String uri, String localName) {
        return elementsToSkip.contains(new QName(uri, localName));
    }

    private static class IgnoringContentHandler extends DefaultHandler {

        private int depth = 1;
        private final XMLReader xmlReader;
        private final ContentHandler contentHandler;

        public IgnoringContentHandler(XMLReader xmlReader, ContentHandler contentHandler) {
            this.contentHandler = contentHandler;
            this.xmlReader = xmlReader;
        }

        @Override
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) {
            depth++;
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            depth--;
            if (0 == depth) {
                xmlReader.setContentHandler(contentHandler);
            }
        }
    }
}
