package no.entur.antu.validator.id;

import java.util.List;
import java.util.Objects;


public class IdVersion {

    private String id;
    private String version;
    private String elementName;
    private List<String> parentElementNames;
    private String filename;
    private int lineNumber;
    private int columnNumber;

    public IdVersion() {

    }

    public IdVersion(String id, String version, String elementName, List<String> parentElementNames, String filename, int lineNumber, int columnNumber) {
        this.id = id;
        this.version = version;
        this.elementName = elementName;
        this.parentElementNames = parentElementNames;
        this.filename = filename;
        this.columnNumber = columnNumber;
        this.lineNumber = lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdVersion idVersion = (IdVersion) o;
        return Objects.equals(id, idVersion.id) && Objects.equals(version, idVersion.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public String toString() {
        return "IdVersion{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", elementName='" + elementName + '\'' +
                ", parentElementNames=" + parentElementNames +
                ", filename='" + filename + '\'' +
                ", lineNumber=" + lineNumber +
                ", columnNumber=" + columnNumber +
                '}';
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getElementName() {
        return elementName;
    }

    public List<String> getParentElementNames() {
        return parentElementNames;
    }

    public String getFilename() {
        return filename;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }


}
