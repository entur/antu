package no.entur.antu.validation.utilities;

import no.entur.antu.validation.NetexCodespace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class CodespaceUtilsTest {

    @Test
    void getValidCodespacesWhenAdditionalCodespacesIsNullOrEmpty() {
        Set<NetexCodespace> codespaces1 = CodespaceUtils.getValidCodespacesFor("nsr", null);
        Assertions.assertEquals(1, codespaces1.size());
        Assertions.assertTrue(codespaces1.contains(NetexCodespace.rutebanken("nsr")));

        Set<NetexCodespace> codespaces2 = CodespaceUtils.getValidCodespacesFor("nsr", Set.of());
        Assertions.assertEquals(1, codespaces2.size());
        Assertions.assertTrue(codespaces2.contains(NetexCodespace.rutebanken("nsr")));
    }

    @Test
    void getValidCodespacesWhenAdditionalCodespacesIsProvided() {
        NetexCodespace additional1 = new NetexCodespace("testCodespace1", "testCodespaceUrl1");
        NetexCodespace additional2 = new NetexCodespace("testCodespace2", "testCodespaceUrl2");

        Set<NetexCodespace> codespaces = CodespaceUtils.getValidCodespacesFor("nsr", Set.of(additional1, additional2));
        Assertions.assertEquals(3, codespaces.size());
        Assertions.assertTrue(codespaces.contains(NetexCodespace.rutebanken("nsr")));
        Assertions.assertTrue(codespaces.contains(additional1));
        Assertions.assertTrue(codespaces.contains(additional2));
    }

}