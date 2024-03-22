package no.entur.antu.validation.utilities;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import net.opengis.gml._3.LinearRingType;
import no.entur.antu.exception.AntuException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for working with JTS geometries,
 * and other general purpose coordinate operations.
 */
public final class GeometryUtilities {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    GeometryUtilities.class
  );

  private static final CoordinateSequenceFactory csf =
    new PackedCoordinateSequenceFactory();
  private static final GeometryFactory gf = new GeometryFactory(csf);

  /**
   * A shared copy of the WGS84 CRS with longitude-first axis order.
   */
  public static final CoordinateReferenceSystem WGS84_XY;

  /*
   * The WGS84 CRS (Coordinate Reference System) with longitude-first axis order.
   */
  static {
    try {
      WGS84_XY =
        CRS
          .getAuthorityFactory(true)
          .createCoordinateReferenceSystem("EPSG:4326");
    } catch (Exception ex) {
      LOGGER.error("Unable to create longitude-first WGS84 CRS", ex);
      throw new AntuException(
        "Could not create longitude-first WGS84 coordinate reference system."
      );
    }
  }

  private GeometryUtilities() {
    // Utility class
  }

  public static List<Double> getCoordinates(LineStringType lineString) {
    return getCoordinates(
      lineString.getPosList(),
      lineString.getPosOrPointProperty()
    );
  }

  public static List<Double> getCoordinates(LinearRingType linearRing) {
    return getCoordinates(
      linearRing.getPosList(),
      linearRing.getPosOrPointProperty()
    );
  }

  private static List<Double> getCoordinates(
    DirectPositionListType posList,
    List<Object> posOrPointProperty
  ) {
    if (posList != null) {
      return posList.getValue();
    } else if (posOrPointProperty != null) {
      return posOrPointProperty
        .stream()
        .filter(DirectPositionType.class::isInstance)
        .map(DirectPositionType.class::cast)
        .map(DirectPositionType::getValue)
        .flatMap(Collection::stream)
        .toList();
    }
    return Collections.emptyList();
  }

  /**
   * Coordinates in Netex coordinates are in the format [lat, lon, lat, lon, ...]
   * but JTS requires the format [lon, lat, lon, lat, ...]
   */
  private static Coordinate[] createJTSCoordinates(List<Double> coordinates) {
    Coordinate[] jtsCoordinates = new Coordinate[coordinates.size() / 2];
    for (int i = 0; i < coordinates.size(); i += 2) {
      jtsCoordinates[i / 2] =
        // Changing the order of the coordinates
        new Coordinate(coordinates.get(i + 1), coordinates.get(i));
    }
    return jtsCoordinates;
  }

  public static LineString createLineStringFromLineStringType(
    List<Double> lineString
  ) {
    return gf.createLineString(createJTSCoordinates(lineString));
  }

  public static CoordinateSequence createCoordinateSequence(
    List<Double> coordinates
  ) {
    Coordinate[] jtsCoordinates = createJTSCoordinates(coordinates);
    return gf.getCoordinateSequenceFactory().create(jtsCoordinates);
  }

  public static LinearRing createLinerRing(List<Double> coordinates) {
    CoordinateSequence coordinateSequence = createCoordinateSequence(
      coordinates
    );
    return gf.createLinearRing(coordinateSequence);
  }

  /**
   * Validating that we have at least two coordinates pairs (4 values)
   * and the list size is even
   */
  public static boolean isValidCoordinatesList(List<Double> coordinates) {
    return coordinates.size() >= 4 && coordinates.size() % 2 == 0;
  }
}
