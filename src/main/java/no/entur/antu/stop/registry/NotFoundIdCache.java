package no.entur.antu.stop.registry;

/**
 * The ids the stop place registry answered 404 for. A dataset referencing an id that does not exist
 * repeats it across its files, and every occurrence after the first answer is recorded would otherwise
 * be another HTTP call.
 *
 * <p>Deliberately not a single-flight: files are validated in parallel across pods, so callers that miss
 * within the same lookup all call the registry. Bounded by how many pods are validating that dataset,
 * against a public read API answering 404s, which is not worth a distributed lock per id in the
 * validation path.
 */
public interface NotFoundIdCache {
  boolean contains(String id);

  /**
   * Remember the id for a while. Entries have to expire: an id missing now is the normal state for a
   * stop place that is about to be created, and a permanent entry would outlive it.
   */
  void remember(String id);
}
