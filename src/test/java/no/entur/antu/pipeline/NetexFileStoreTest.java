package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import no.entur.antu.memorystore.TemporaryFileRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NetexFileStoreTest {

  private final TemporaryFileRepository temporaryFileRepository = mock(
    TemporaryFileRepository.class
  );
  private final NetexFileStore store = new NetexFileStore(
    temporaryFileRepository
  );

  @Test
  void aFileSurvivesTheRoundTrip() {
    byte[] content = "<PublicationDelivery/>".getBytes(StandardCharsets.UTF_8);

    store.save("reportId", "line.xml", content);

    ArgumentCaptor<byte[]> stored = ArgumentCaptor.forClass(byte[].class);
    verify(temporaryFileRepository)
      .upload(eq("reportId"), eq("line.xml"), stored.capture());

    when(temporaryFileRepository.download("reportId", "line.xml"))
      .thenReturn(stored.getValue());
    assertArrayEquals(content, store.read("reportId", "line.xml"));
  }

  /**
   * Redis memory is the constraint that decides how many datasets can be validated at once, so the
   * files are stored compressed.
   */
  @Test
  void theStoredFormIsCompressed() {
    byte[] repetitive =
      "<StopPointInJourneyPattern/>".repeat(500)
        .getBytes(StandardCharsets.UTF_8);

    byte[] zipped = NetexFileStore.zipSingleEntry("line.xml", repetitive);

    assertTrue(
      zipped.length < repetitive.length / 2,
      "expected compression, got " +
      zipped.length +
      " bytes from " +
      repetitive.length
    );
  }

  @Test
  void theEntryKeepsTheNetexFileName() throws Exception {
    byte[] zipped = NetexFileStore.zipSingleEntry(
      "_common.xml",
      "x".getBytes()
    );

    try (
      ZipInputStream archive = new ZipInputStream(
        new java.io.ByteArrayInputStream(zipped)
      )
    ) {
      ZipEntry entry = archive.getNextEntry();
      assertEquals("_common.xml", entry.getName());
    }
  }

  @Test
  void anEmptyFileRoundTrips() {
    assertArrayEquals(
      new byte[0],
      NetexFileStore.unzipSingleEntry(
        NetexFileStore.zipSingleEntry("empty.xml", new byte[0])
      )
    );
  }
}
