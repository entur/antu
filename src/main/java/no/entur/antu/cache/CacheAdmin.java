package no.entur.antu.cache;

/**
 * Interface to control the shared object cache.
 */
public interface CacheAdmin {
  /**
   * Clear all elements in the cache.
   */
  void clear();

  /**
   * Return the list of keys present in the cache.
   *
   * @return the list of keys present in the cache.
   */
  String dumpKeys();
}
