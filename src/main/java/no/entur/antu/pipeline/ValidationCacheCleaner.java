package no.entur.antu.pipeline;

import no.entur.antu.memorystore.TemporaryFileRepository;
import no.entur.antu.netexdata.NetexDataRepositoryLoader;
import no.entur.antu.sweden.stop.SwedenStopPlaceNetexIdRepository;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.entur.netex.validation.validator.jaxb.CommonDataRepositoryLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Drops everything Redis holds for one validation.
 *
 * <p>Removing the validation state is also what makes a redelivered job a no-op: a job whose report
 * has no state left has already run.
 */
@Component
public class ValidationCacheCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ValidationCacheCleaner.class
  );

  private final CommonDataRepositoryLoader commonDataRepository;
  private final NetexIdRepository netexIdRepository;
  private final TemporaryFileRepository temporaryFileRepository;
  private final SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository;
  private final NetexDataRepositoryLoader netexDataRepository;
  private final ValidationStateRepository validationStateRepository;
  private final ValidationBarrier validationBarrier;

  public ValidationCacheCleaner(
    CommonDataRepositoryLoader commonDataRepository,
    NetexIdRepository netexIdRepository,
    TemporaryFileRepository temporaryFileRepository,
    SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository,
    NetexDataRepositoryLoader netexDataRepository,
    ValidationStateRepository validationStateRepository,
    ValidationBarrier validationBarrier
  ) {
    this.commonDataRepository = commonDataRepository;
    this.netexIdRepository = netexIdRepository;
    this.temporaryFileRepository = temporaryFileRepository;
    this.swedenStopPlaceNetexIdRepository = swedenStopPlaceNetexIdRepository;
    this.netexDataRepository = netexDataRepository;
    this.validationStateRepository = validationStateRepository;
    this.validationBarrier = validationBarrier;
  }

  public void cleanUp(String validationReportId) {
    LOGGER.info("Cleaning up the cache");
    commonDataRepository.cleanUp(validationReportId);
    netexIdRepository.cleanUp(validationReportId);
    temporaryFileRepository.cleanUp(validationReportId);
    swedenStopPlaceNetexIdRepository.cleanUp(validationReportId);
    netexDataRepository.cleanUp(validationReportId);
    validationBarrier.cleanUp(validationReportId);
    validationStateRepository.cleanUp(validationReportId);
    LOGGER.info("Cleaned up the cache");
  }
}
