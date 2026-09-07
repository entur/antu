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
   * Stop whatever {@link #init()} started, because this pod is no longer the leader.
   */
  default void standDown() {}

  /**
   * Create the stop place repository if it is empty, otherwise update the stop place repository.
   */
  void createOrUpdate();
}
