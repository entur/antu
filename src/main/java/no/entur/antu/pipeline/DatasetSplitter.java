package no.entur.antu.pipeline;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import no.entur.antu.exception.AntuException;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.services.AntuExchangeBlobStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Explodes a NeTEx dataset archive into single files and creates the validation jobs for them.
 *
 * <p>Common files, the ones whose name starts with an underscore, are validated first: rules applied
 * to a line file may need data the common files declare. The line file jobs are therefore created
 * only once every common file has been validated, which is what
 * {@link ValidationBarrier.Stage#COMMON_FILES_VALIDATED} waits for. A dataset without common files
 * skips the barrier entirely.
 */
@Component
public class DatasetSplitter {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DatasetSplitter.class
  );

  private static final String NETEX_FILE_SUFFIX = ".xml";
  private static final int PROGRESS_LOG_INTERVAL = 100;

  private final AntuExchangeBlobStoreService antuExchangeBlobStoreService;
  private final NetexFileStore netexFileStore;
  private final JobQueue jobQueue;

  public DatasetSplitter(
    AntuExchangeBlobStoreService antuExchangeBlobStoreService,
    NetexFileStore netexFileStore,
    JobQueue jobQueue
  ) {
    this.antuExchangeBlobStoreService = antuExchangeBlobStoreService;
    this.netexFileStore = netexFileStore;
    this.jobQueue = jobQueue;
  }

  public void split(AntuJob.SplitDataset job) {
    ValidationContext context = job.context();
    String datasetFileHandle = context.datasetFileHandle();

    LOGGER.info("Downloading NeTEx dataset {}", datasetFileHandle);
    InputStream dataset = antuExchangeBlobStoreService.getBlob(
      datasetFileHandle
    );
    if (dataset == null) {
      LOGGER.error("NeTEx dataset not found: {}", datasetFileHandle);
      return;
    }

    long startedAt = System.currentTimeMillis();
    List<String> netexFileNames = storeSingleNetexFiles(context, dataset);
    LOGGER.info(
      "Split the NeTEx dataset into {} files in {} ms",
      netexFileNames.size(),
      System.currentTimeMillis() - startedAt
    );

    // Reverse name order, so that a common file declaring shared data is validated before one
    // referencing it: _stops.xml has to be seen before _shared_data.xml, or the references to its stop
    // places are reported as unresolved. The dependency is really between the data, not the names, so a
    // dataset that does not follow the convention will report spurious unresolved references.
    List<String> commonFileNames = netexFileNames
      .stream()
      .filter(AntuJob::isCommonFile)
      .sorted(Comparator.reverseOrder())
      .toList();
    if (commonFileNames.isEmpty()) {
      createLineFileJobs(
        new AntuJob.CreateLineFileJobs(context, netexFileNames)
      );
    } else {
      createValidationJobs(
        context,
        commonFileNames,
        netexFileNames,
        commonFileNames.size()
      );
    }
  }

  /**
   * Create a validation job for every line file, now that the common files are validated.
   */
  public void createLineFileJobs(AntuJob.CreateLineFileJobs job) {
    List<String> lineFileNames = job
      .allNetexFileNames()
      .stream()
      .filter(fileName -> !AntuJob.isCommonFile(fileName))
      .toList();
    createValidationJobs(
      job.context(),
      lineFileNames,
      job.allNetexFileNames(),
      0
    );
  }

  /**
   * @param nbCommonFiles the common files barrier target, only carried on common file jobs.
   */
  private void createValidationJobs(
    ValidationContext context,
    List<String> fileNamesToValidate,
    List<String> allNetexFileNames,
    int nbCommonFiles
  ) {
    LOGGER.info("Creating {} validation jobs", fileNamesToValidate.size());
    for (String fileName : fileNamesToValidate) {
      // Only common files need the file name list: they carry it to the pod that passes the common
      // files barrier and has to create the line file jobs from it.
      List<String> carriedFileNames = AntuJob.isCommonFile(fileName)
        ? allNetexFileNames
        : List.of();
      jobQueue.submit(
        new AntuJob.ValidateFile(
          context,
          fileName,
          allNetexFileNames.size(),
          nbCommonFiles,
          carriedFileNames
        )
      );
    }
  }

  /**
   * Stream the archive entry by entry, filing each NeTEx file in the memory store. Streaming keeps
   * the memory footprint at one entry regardless of how large the dataset is.
   *
   * @return the names of the NeTEx files found, in ascending name order.
   */
  private List<String> storeSingleNetexFiles(
    ValidationContext context,
    InputStream dataset
  ) {
    List<String> netexFileNames = new ArrayList<>();
    try (ZipInputStream archive = new ZipInputStream(dataset)) {
      ZipEntry entry;
      int entryCount = 0;
      while ((entry = archive.getNextEntry()) != null) {
        if (++entryCount % PROGRESS_LOG_INTERVAL == 0) {
          LOGGER.info("Uploaded {} NeTEx files", entryCount);
        }
        if (isNetexFile(entry)) {
          String fileName = entry.getName();
          netexFileStore.save(
            context.validationReportId(),
            fileName,
            readEntry(archive)
          );
          netexFileNames.add(fileName);
        }
      }
    } catch (IOException e) {
      throw new AntuException(
        "Failed to read the NeTEx dataset " + context.datasetFileHandle(),
        e
      );
    }
    return netexFileNames.stream().sorted().toList();
  }

  private static boolean isNetexFile(ZipEntry entry) {
    if (entry.isDirectory()) {
      return false;
    }
    if (!entry.getName().endsWith(NETEX_FILE_SUFFIX)) {
      LOGGER.debug("Ignoring non-XML file {}", entry.getName());
      return false;
    }
    return true;
  }

  private static byte[] readEntry(ZipInputStream archive) throws IOException {
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    archive.transferTo(content);
    return content.toByteArray();
  }
}
