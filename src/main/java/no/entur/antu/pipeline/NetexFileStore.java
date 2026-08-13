package no.entur.antu.pipeline;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import no.entur.antu.exception.AntuException;
import no.entur.antu.memorystore.TemporaryFileRepository;
import org.springframework.stereotype.Component;

/**
 * Holds the single NeTEx files a dataset was split into, for the duration of the validation.
 *
 * <p>The files live in the memory store rather than in a bucket because every pod validating the
 * dataset reads them, and Redis round trips are an order of magnitude cheaper than GCS ones. They
 * are stored zipped: a NeTEx line file compresses by roughly a factor of ten, and Redis memory is
 * the scarce resource.
 */
@Component
public class NetexFileStore {

  private final TemporaryFileRepository temporaryFileRepository;

  public NetexFileStore(TemporaryFileRepository temporaryFileRepository) {
    this.temporaryFileRepository = temporaryFileRepository;
  }

  public void save(String validationReportId, String fileName, byte[] content) {
    temporaryFileRepository.upload(
      validationReportId,
      fileName,
      zipSingleEntry(fileName, content)
    );
  }

  /**
   * @throws no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException if the file is gone,
   *         which is how a duplicated PubSub delivery is recognised: the temporary files are
   *         deleted once the validation is complete.
   */
  public byte[] read(String validationReportId, String fileName) {
    return unzipSingleEntry(
      temporaryFileRepository.download(validationReportId, fileName)
    );
  }

  static byte[] zipSingleEntry(String entryName, byte[] content) {
    ByteArrayOutputStream zipped = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(zipped)) {
      zipOutputStream.putNextEntry(new ZipEntry(entryName));
      zipOutputStream.write(content);
      zipOutputStream.closeEntry();
    } catch (IOException e) {
      throw new AntuException("Failed to compress " + entryName, e);
    }
    return zipped.toByteArray();
  }

  static byte[] unzipSingleEntry(byte[] zipped) {
    try (
      ZipInputStream zipInputStream = new ZipInputStream(
        new ByteArrayInputStream(zipped)
      )
    ) {
      if (zipInputStream.getNextEntry() == null) {
        throw new AntuException("Empty archive in the memory store");
      }
      return readAllBytes(zipInputStream);
    } catch (IOException e) {
      throw new AntuException("Failed to decompress a NeTEx file", e);
    }
  }

  private static byte[] readAllBytes(InputStream inputStream)
    throws IOException {
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    inputStream.transferTo(content);
    return content.toByteArray();
  }
}
