package no.entur.antu.stop.loader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import no.entur.antu.exception.AntuException;
import no.entur.antu.services.MardukBlobStoreService;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
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
    NetexParser netexParser = new NetexParser();
    while (zipEntry != null) {
      byte[] allBytes = zipInputStream.readAllBytes();
      netexParser.parse(new ByteArrayInputStream(allBytes), index);
      zipEntry = zipInputStream.getNextEntry();
    }
  }
}
