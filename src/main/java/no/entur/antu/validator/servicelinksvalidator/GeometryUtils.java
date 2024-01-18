package no.entur.antu.validator.servicelinksvalidator;

import no.entur.antu.exception.AntuException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeometryUtils {

  private static final Logger LOG = LoggerFactory.getLogger(GeometryUtils.class);
  private static final CoordinateSequenceFactory csf = new PackedCoordinateSequenceFactory();
  private static final GeometryFactory gf = new GeometryFactory(csf);

  /**
   * A shared copy of the WGS84 CRS with longitude-first axis order.
   */
  public static final CoordinateReferenceSystem WGS84_XY;

  static {
    try {
      WGS84_XY = CRS.getAuthorityFactory(true).createCoordinateReferenceSystem("EPSG:4326");
    } catch (Exception ex) {
      LOG.error("Unable to create longitude-first WGS84 CRS", ex);
      throw new AntuException(
        "Could not create longitude-first WGS84 coordinate reference system."
      );
    }
  }

  private GeometryUtils() {
  }

  public static GeometryFactory getGeometryFactory() {
    return gf;
  }
}
