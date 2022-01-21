package no.entur.antu.validator.codespace;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static no.entur.antu.Constants.NSR_XMLNS;
import static no.entur.antu.Constants.NSR_XMLNSURL;
import static no.entur.antu.Constants.PEN_XMLNS;
import static no.entur.antu.Constants.PEN_XMLNSURL;

/**
 * A NeTEx codespace, identified by its namespace and its URL.
 */
public class NetexCodespace {

    public static final NetexCodespace NSR_NETEX_CODESPACE = new NetexCodespace(NSR_XMLNS, NSR_XMLNSURL);
    public static final NetexCodespace PEN_NETEX_CODESPACE = new NetexCodespace(PEN_XMLNS, PEN_XMLNSURL);

    private final String xmlns;
    private final String xmlnsUrl;

    public NetexCodespace(String xmlns, String xmlnsUrl) {
        this.xmlns = xmlns;
        this.xmlnsUrl = xmlnsUrl;
    }


    public static Set<NetexCodespace> getValidNetexCodespacesFor(String codespace) {
        NetexCodespace netexCodespace = NetexCodespace.getNetexCodespaceFor(codespace);
        return  Set.of(NSR_NETEX_CODESPACE, PEN_NETEX_CODESPACE, netexCodespace);
    }

    private static NetexCodespace getNetexCodespaceFor(String codespace) {
        return new NetexCodespace(codespace, "http://www.rutebanken.org/ns/" + codespace.toLowerCase(Locale.ROOT));
    }

    public String getXmlns() {
        return xmlns;
    }

    public String getXmlnsUrl() {
        return xmlnsUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetexCodespace netexCodespace = (NetexCodespace) o;
        return Objects.equals(xmlns, netexCodespace.xmlns) && Objects.equals(xmlnsUrl, netexCodespace.xmlnsUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xmlns, xmlnsUrl);
    }

    @Override
    public String toString() {
        return "{" +
                "xmlns='" + xmlns + '\'' +
                ", xmlnsUrl='" + xmlnsUrl + '\'' +
                '}';
    }

}
