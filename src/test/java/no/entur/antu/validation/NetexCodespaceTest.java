package no.entur.antu.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetexCodespaceTest {

    @Test
    void testRutebankenSpecializedCodespace() {
        NetexCodespace codespace = NetexCodespace.rutebanken("NSR");
        assertEquals("NSR", codespace.xmlns());
        assertEquals("http://www.rutebanken.org/ns/nsr", codespace.xmlnsUrl());
    }

    @Test
    void testGeneralCodespace() {
        NetexCodespace codespace = new NetexCodespace("testXmlns", "testXmlnsUrl");
        assertEquals("testXmlns", codespace.xmlns());
        assertEquals("testXmlnsUrl", codespace.xmlnsUrl());
    }

}