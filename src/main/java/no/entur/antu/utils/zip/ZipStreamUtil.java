package no.entur.antu.utils.zip;

import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility class for handling ZIP files as streams.
 * Provides methods to process ZIP entries sequentially without loading the entire ZIP into memory.
 */
public class ZipStreamUtil {

  private static final Logger LOG = LoggerFactory.getLogger(
    ZipStreamUtil.class
  );

  private ZipStreamUtil() {
    // Utility class, prevent instantiation
  }

  /**
   * Creates a stream of InputStreams from a ZIP file.
   * Each InputStream in the returned stream represents a single file from the ZIP archive.
   * This method processes ZIP entries sequentially to minimize memory usage.
   *
   * @param zipInputStream the input stream containing ZIP data
   * @return a stream of InputStreams, each representing a file from the ZIP
   */
  public static Stream<InputStream> createInputStreamFromZip(
    InputStream zipInputStream
  ) {
    // Create a ZipArchiveInputStream from the original input stream
    final ZipArchiveInputStream zipInput = new ZipArchiveInputStream(
      zipInputStream
    );

    return StreamSupport
      .stream(
        Spliterators.spliteratorUnknownSize(
          new ZipEntryIterator(zipInput),
          Spliterator.ORDERED | Spliterator.NONNULL
        ),
        false
      )
      .onClose(() -> {
        try {
          zipInput.close();
          zipInputStream.close();
        } catch (IOException e) {
          LOG.error("Error closing ZIP stream", e);
        }
      });
  }
}
