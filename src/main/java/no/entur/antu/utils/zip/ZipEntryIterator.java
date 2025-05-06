package no.entur.antu.utils.zip;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator implementation for processing ZIP entries sequentially.
 * This iterator returns an InputStream for each file in the ZIP archive.
 */
class ZipEntryIterator implements Iterator<InputStream> {

  private static final Logger LOG = LoggerFactory.getLogger(
    ZipEntryIterator.class
  );

  private final ZipArchiveInputStream zipInput;
  private ZipArchiveEntry entry;
  private boolean entryReturned = false;

  /**
   * Creates a new iterator for the given ZIP input stream.
   *
   * @param zipInput the ZIP archive input stream to iterate over
   */
  ZipEntryIterator(ZipArchiveInputStream zipInput) {
    this.zipInput = zipInput;
  }

  @Override
  public boolean hasNext() {
    if (entry != null && !entryReturned) {
      return true;
    }

    try {
      do {
        entry = zipInput.getNextZipEntry();
      } while (entry != null && entry.isDirectory());

      entryReturned = false;
      return entry != null;
    } catch (IOException e) {
      LOG.error("Error reading ZIP entry", e);
      try {
        zipInput.close();
      } catch (IOException closeEx) {
        LOG.error("Error closing ZIP stream", closeEx);
      }
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public InputStream next() {
    if (!hasNext()) {
      throw new NoSuchElementException("No more entries in ZIP file");
    }

    entryReturned = true;
    final String entryName = entry.getName();
    final long entrySize = entry.getSize();

    LOG.debug("Processing file: {} (size: {})", entryName, entrySize);

    // Create a bounded input stream that reads only the current entry's data
    return new ZipEntryInputStream(zipInput, entryName, entrySize);
  }
}
