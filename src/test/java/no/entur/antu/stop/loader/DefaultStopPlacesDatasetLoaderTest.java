package no.entur.antu.stop.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import no.entur.antu.services.MardukBlobStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.storage.repository.InMemoryBlobStoreRepository;

class DefaultStopPlacesDatasetLoaderTest {

  private static final String DATASET = "tiamat/CurrentAndFuture_latest.zip";
  private static final String EXPORT =
    "tiamat-export-CurrentAndFuture-202608211200010574.xml";

  private InMemoryBlobStoreRepository blobStore;
  private DefaultStopPlacesDatasetLoader loader;

  @BeforeEach
  void setUp() {
    blobStore = new InMemoryBlobStoreRepository(new HashMap<>());
    loader =
      new DefaultStopPlacesDatasetLoader(
        new MardukBlobStoreService("marduk", blobStore),
        DATASET
      );
  }

  /**
   * The version has to come out of the archive without parsing it: this is polled, and the dataset is
   * hundreds of megabytes of XML.
   */
  @Test
  void theDatasetVersionIsTheNameOfTheExportInTheArchive() throws IOException {
    blobStore.uploadBlob(DATASET, zipContaining(EXPORT));

    assertEquals(EXPORT, loader.loadDatasetVersion());
  }

  @Test
  void aMissingDatasetHasNoVersion() {
    assertNull(loader.loadDatasetVersion());
  }

  private static ByteArrayInputStream zipContaining(String entryName)
    throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry(entryName));
      zip.write("<PublicationDelivery/>".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return new ByteArrayInputStream(bytes.toByteArray());
  }
}
