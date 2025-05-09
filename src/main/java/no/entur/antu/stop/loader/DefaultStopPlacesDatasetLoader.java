package no.entur.antu.stop.loader;

import static org.entur.netex.validation.xml.NetexXMLParser.NETEX_NAMESPACE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import no.entur.antu.exception.AntuException;
import no.entur.antu.services.MardukBlobStoreService;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.xml.SkippingXMLStreamReaderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultStopPlacesDatasetLoader implements StopPlacesDatasetLoader {

  private final MardukBlobStoreService mardukBlobStoreService;
  private final String currentStopPlacesFile;

  public DefaultStopPlacesDatasetLoader(
    MardukBlobStoreService mardukBlobStoreService,
    @Value(
      "${antu.netex.stop.current.filename:tiamat/CurrentAndFuture_latest.zip}"
    ) String currentStopPlacesFile
  ) {
    this.mardukBlobStoreService = mardukBlobStoreService;
    this.currentStopPlacesFile = currentStopPlacesFile;
  }

  @Override
  public NetexEntitiesIndex loadNetexEntitiesIndex() {
    InputStream currentStopPlaceBlob = mardukBlobStoreService.getBlob(
      currentStopPlacesFile
    );
    try (
      ZipInputStream zipInputStream = new ZipInputStream(currentStopPlaceBlob)
    ) {
      NetexEntitiesIndex index = new NetexEntitiesIndexImpl();
      parseDataset(zipInputStream, index);
      return index;
    } catch (IOException e) {
      throw new AntuException(
        "Error while parsing the NeTEx timetable dataset",
        e
      );
    }
  }

  /**
   * Parse a zip file containing a NeTEx archive.
   *
   * @param zipInputStream a stream on a NeTEx zip archive.
   * @param index in memory netex model
   * @throws IOException if the zip file cannot be read.
   */
  private void parseDataset(
    ZipInputStream zipInputStream,
    NetexEntitiesIndex index
  ) throws IOException {
    ZipEntry zipEntry = zipInputStream.getNextEntry();
    NetexParser netexParser = new NetexParser(this::filteredXmlStreamReader);
    while (zipEntry != null) {
      byte[] allBytes = zipInputStream.readAllBytes();
      netexParser.parse(new ByteArrayInputStream(allBytes), index);
      zipEntry = zipInputStream.getNextEntry();
    }
  }

  /**
   * Ignore scheduled stop points, fare zones and topographic places.
   */
  private XMLStreamReader filteredXmlStreamReader(InputStream stream) {
    try {
      return SkippingXMLStreamReaderFactory.newXMLStreamReader(
        stream,
        Set.of(
          new QName(NETEX_NAMESPACE, "scheduledStopPoints"),
          new QName(NETEX_NAMESPACE, "fareZones"),
          new QName(NETEX_NAMESPACE, "topographicPlaces")
        )
      );
    } catch (XMLStreamException e) {
      throw new AntuException("Cannot create XMLStreamReader", e);
    }
  }
}
