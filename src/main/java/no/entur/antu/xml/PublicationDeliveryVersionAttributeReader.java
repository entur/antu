package no.entur.antu.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static no.entur.antu.xml.XMLParserUtil.getSecureXmlInputFactory;


public class PublicationDeliveryVersionAttributeReader {


    private static final Logger LOGGER = LoggerFactory.getLogger(PublicationDeliveryVersionAttributeReader.class);

    private PublicationDeliveryVersionAttributeReader() {
    }

    public static String findPublicationDeliveryVersion(byte[] content) {

        String versionAttribute = null;
        try {
            // First create a new XMLInputFactory
            XMLInputFactory inputFactory = getSecureXmlInputFactory();
            // Setup a new eventReader
            InputStream in = new ByteArrayInputStream(content);
            XMLEventReader eventReader = inputFactory.createXMLEventReader(new BufferedInputStream(in));
            // Read the XML document

            while (versionAttribute == null && eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();

                    if (startElement.getName().getLocalPart().equals("PublicationDelivery")) {

                        Iterator<Attribute> attributes = event.asStartElement().getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();
                            if (attribute.getName().toString().equals("version")) {
                                versionAttribute = attribute.getValue();
                            }
                        }

                    }

                }
            }
            eventReader.close();
            in.close();
        } catch (XMLStreamException e) {
            LOGGER.error("Malformed xml", e);
        } catch (IOException e) {
            LOGGER.error("Error closing file", e);
        }

        return versionAttribute;
    }

}
