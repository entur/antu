package no.entur.antu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.storage.repository.InMemoryBlobStoreRepository;

/**
 * The version has to come from the blob's metadata. Reading it through {@code getBlob} would download the
 * whole stop place dataset, twice, on every poll.
 */
class MardukBlobStoreServiceTest {

  private static final String DATASET = "tiamat/CurrentAndFuture_latest.zip";
  private static final BlobId BLOB_ID = BlobId.of("marduk", DATASET);
  private static final Storage.BlobGetOption GENERATION_ONLY =
    Storage.BlobGetOption.fields(Storage.BlobField.GENERATION);

  @Test
  void theVersionIsTheBlobGeneration() {
    Storage storage = mock(Storage.class);
    Blob blob = mock(Blob.class);
    when(blob.getGeneration()).thenReturn(1787306879708878L);
    when(storage.get(eq(BLOB_ID), eq(GENERATION_ONLY))).thenReturn(blob);

    assertEquals("1787306879708878", service(storage).blobVersion(DATASET));
  }

  @Test
  void aBlobThatIsNotThereHasNoVersion() {
    Storage storage = mock(Storage.class);
    when(storage.get(eq(BLOB_ID), eq(GENERATION_ONLY))).thenReturn(null);

    assertNull(service(storage).blobVersion(DATASET));
  }

  /**
   * The in-memory and local-disk stores keep no metadata. They still have to answer that the dataset is
   * there, or it is never loaded at all; a constant is enough for that, and the price is not noticing the
   * file change afterwards.
   */
  @Test
  void aStoreThatCannotVersionStillReportsThatTheBlobIsThere() {
    MardukBlobStoreService service = serviceWithoutGcs();
    assertNull(service.blobVersion(DATASET));

    service.uploadBlob(DATASET, new ByteArrayInputStream(new byte[] { 1 }));

    assertEquals("unversioned", service.blobVersion(DATASET));
  }

  private static MardukBlobStoreService serviceWithoutGcs() {
    return new MardukBlobStoreService(
      "marduk",
      new InMemoryBlobStoreRepository(new HashMap<>()),
      Optional.empty()
    );
  }

  private static MardukBlobStoreService service(Storage storage) {
    return new MardukBlobStoreService(
      "marduk",
      new InMemoryBlobStoreRepository(new HashMap<>()),
      Optional.of(storage)
    );
  }
}
