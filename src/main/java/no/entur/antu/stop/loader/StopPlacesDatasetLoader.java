package no.entur.antu.stop.loader;

import javax.annotation.Nullable;
import org.entur.netex.index.api.NetexEntitiesIndex;

/**
 * Load and parse a NeTEx dataset.
 */
public interface StopPlacesDatasetLoader {
  /**
   *  Return an index over JAXB entities extracted from a NeTEx dataset.
   */
  NetexEntitiesIndex loadNetexEntitiesIndex();

  /**
   * Return an identifier for the dataset now in the bucket, or null if there is none or the store cannot
   * tell. Cheap enough to poll: it reads the blob's metadata, not the dataset.
   */
  @Nullable
  default String loadDatasetVersion() {
    return null;
  }
}
