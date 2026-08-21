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
   * Return an identifier for the dataset now in the bucket, or null if it is not there. Tiamat stamps
   * the export time into the name of the single entry in the archive, so this reads one zip header
   * rather than parsing the dataset, and is cheap enough to poll.
   */
  @Nullable
  String loadDatasetVersion();
}
