package no.entur.antu.netexdata;

import no.entur.antu.netex.test.repository.TestNetexDataRepository;

/**
 * The loader half of the NeTEx data repository fake.
 *
 * <p>cleanUp is the only method on {@link NetexDataRepositoryLoader} that the library's
 * NetexDataRepository does not already declare, so it is the only part of the fake that is
 * antu-specific. Everything else lives in {@link TestNetexDataRepository}, which is destined for
 * netex-validator-java and must not import anything from antu.
 */
public class TestNetexDataRepositoryLoader
  extends TestNetexDataRepository
  implements NetexDataRepositoryLoader {

  @Override
  public void cleanUp(String validationReportId) {}
}
