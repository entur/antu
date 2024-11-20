package no.entur.antu.netexdata;

import org.entur.netex.validation.validator.jaxb.NetexDataRepository;

/**
 * This interface extends the read-only interface {@link NetexDataRepository} with methods for cleaning up the repository.
 */
public interface NetexDataRepositoryLoader extends NetexDataRepository {
  /**
   * Clean up the NeTEx data repository for the given validation report.
   */
  void cleanUp(String validationReportId);
}
