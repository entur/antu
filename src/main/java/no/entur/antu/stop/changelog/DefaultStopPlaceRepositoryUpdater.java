package no.entur.antu.stop.changelog;

import no.entur.antu.stop.StopPlaceRepositoryLoader;

/**
 * Load the stop repository from a NeTEx archive.
 */
public class DefaultStopPlaceRepositoryUpdater
  implements StopPlaceRepositoryUpdater {

  private final StopPlaceRepositoryLoader stopPlaceRepositoryLoader;

  public DefaultStopPlaceRepositoryUpdater(
    StopPlaceRepositoryLoader stopPlaceRepositoryLoader
  ) {
    this.stopPlaceRepositoryLoader = stopPlaceRepositoryLoader;
  }

  @Override
  public void createOrUpdate() {
    stopPlaceRepositoryLoader.refreshCache();
  }
}
