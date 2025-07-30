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

  /**
   * Get detailed content of a specific cache key.
   * Returns the actual data stored in the cache for debugging purposes.
   */
  String inspectKey(String key);

  /**
   * Get keys matching a pattern with their content preview.
   * Useful for debugging cache content for specific validation reports.
   */
  String inspectKeysByPattern(String pattern);
}
