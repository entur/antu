/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.entur.antu.stop;

import no.entur.antu.model.QuayId;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.StopPlaceId;
import no.entur.antu.model.TransportModes;

/**
 * A repository to store and cache the stop place and quay ids retrieved from the National Stop Register.
 */
public interface StopPlaceRepository {
  /**
   * Checks if stop place id present in the cache or try getting it from Read api.
   * @return stop place id.
   */
  boolean hasStopPlaceId(StopPlaceId stopPlaceId);

  /**
   * Checks if quay id present in the cache or try getting it from Read api.
   * @return stop place id.
   */
  boolean hasQuayId(QuayId quayId);

  /**
   * Returns the transport modes for quay id present in the cache or try getting it from Read api.
   * @return transport modes for given stop place id.
   */
  TransportModes getTransportModesForQuayId(QuayId quayId);

  /**
   * Returns the coordinates for quay id present in the cache or try getting it from Read api.
   * @return coordinates for given stop place id.
   */
  QuayCoordinates getCoordinatesForQuayId(QuayId quayId);

  /**
   * Refresh the cache with data retrieved from the Stop Place Register.
   */
  void refreshCache();
}
