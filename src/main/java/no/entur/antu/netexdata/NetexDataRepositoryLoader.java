package no.entur.antu.netexdata;

import org.entur.netex.validation.validator.jaxb.NetexDataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * This interface extends the read-only interface {@link NetexDataRepository} with methods for cleaning up the repository.
 */
public interface NetexDataRepositoryLoader extends NetexDataRepository {
  /**
   * Clean up the NeTEx data repository for the given validation report.
   */
  void cleanUp(String validationReportId);

  Map<String, List<LocalDateTime>> serviceJourneyIdToActiveDates(String validationReportId);
}
