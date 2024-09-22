package no.entur.antu.stop.loader;

import org.entur.netex.index.api.NetexEntitiesIndex;

/**
 * Load and parse a NeTEx dataset.
 */
public interface StopPlacesDatasetLoader {
  /**
   *  Return an index over JAXB entities extracted from a NeTEx dataset.
   */
  NetexEntitiesIndex loadNetexEntitiesIndex();
}
