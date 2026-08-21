package no.entur.antu.stop.changelog;

/**
 * Update the stop place repository.
 */
public interface StopPlaceRepositoryUpdater {
  /**
   * Initialize the updater.
   */
  default void init() {}

  /**
   * Create the stop place repository if it is empty, otherwise update the stop place repository.
   */
  void createOrUpdate();

  /**
   * Restart whatever feeds real-time updates into the repository if it has stopped. A no-op for
   * updaters that only read the NeTEx export.
   */
  default void ensureRunning() {}
}
