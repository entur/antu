package no.entur.antu.utils.zip;

import org.apache.commons.io.input.BoundedInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * A specialized InputStream for reading a single entry from a ZIP archive.
 * This class extends BoundedInputStream to limit reading to just the current ZIP entry's data,
 * while ensuring proper handling of entry boundaries and resource cleanup.
 */
class ZipEntryInputStream extends BoundedInputStream {

  private static final Logger LOG = LoggerFactory.getLogger(
    ZipEntryInputStream.class
  );

  private final String entryName;
  private boolean closed = false;

  /**
   * Creates a new input stream for reading a specific ZIP entry.
   *
   * @param in the underlying ZIP input stream
   * @param entryName the name of the ZIP entry being read
   * @param entrySize the size of the ZIP entry in bytes
   */
  ZipEntryInputStream(InputStream in, String entryName, long entrySize) {
    super(in, entrySize >= 0 ? entrySize : Long.MAX_VALUE);
    this.entryName = entryName;
  }

  @Override
  public int read() throws IOException {
    if (closed) return -1;
    int result = super.read();
    if (result == -1) close();
    return result;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (closed) return -1;
    int result = super.read(b, off, len);
    if (result == -1) close();
    return result;
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      closed = true;
      LOG.debug("Finished processing file: {}", entryName);
      // Don't close the underlying stream, just consume the rest of this entry if needed
      while (super.read() != -1) {}
    }
  }
}
