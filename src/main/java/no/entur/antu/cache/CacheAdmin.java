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
   * Delete keys by pattern and return the number of deleted keys
   */
  long deleteKeysByPattern(String pattern);

  /**
   * Return the list of keys present in the cache.
   */
  String dumpKeys();
}
