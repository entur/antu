package no.entur.antu.flex.validation.validator.flexibleareavalidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.opengis.gml._3.AbstractRingType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LinearRingType;
import no.entur.antu.validator.servicelinksvalidator.GeometryUtils;
import org.entur.netex.index.api.NetexEntityIndex;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexibleAreaContextBuilder {

  public record FlexibleAreaContext(
    String flexibleStopPlaceId,
    String flexibleAreaId,
    LinearRing linearRing
  ) {}

  private static final Logger LOGGER = LoggerFactory.getLogger(
    FlexibleAreaContextBuilder.class
  );

/*
  public List<FlexibleAreaContext> build(NetexEntityIndex<FlexibleStopPlace> flexibleStopPlaceIndex) {
    return flexibleStopPlaceIndex
      .getAll()
      .stream()
      .flatMap(flexibleStopPlace -> flexibleStopPlace
        .getAreas()
        .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
        .stream()
        .filter(FlexibleArea.class::isInstance)
        .map(FlexibleArea.class::cast)
        .filter(flexibleArea -> flexibleArea
          .getPolygon()
          .getExterior()
          .getAbstractRing()
          .getValue() instanceof LinearRingType)
        .map(flexibleArea -> {
          LinearRingType linearRingType = (LinearRingType) flexibleArea
            .getPolygon()
            .getExterior()
            .getAbstractRing()
            .getValue();
          List<Double> coordinates = getCoordinates(linearRingType);
          Coordinate[] jtsCoordinates = createJTSCoordinates(coordinates);
          LinearRing linearRing = GeometryUtils
            .getGeometryFactory()
            .createLinearRing(jtsCoordinates);
          return new FlexibleAreaContext(
            flexibleArea
              .getFlexibleStopPlaceRef()
              .getRef(),
            flexibleArea.getId(),
            linearRing
          );
        }))
      .toList();
  }
*/


  public List<FlexibleAreaContext> build(NetexEntityIndex<FlexibleStopPlace> flexibleStopPlaceIndex) {
    List<FlexibleAreaContext> flexibleAreaContexts = new ArrayList<>();

    List<FlexibleStopPlace> flexibleStopPlaces = flexibleStopPlaceIndex
      .getAll()
      .stream()
      .toList();

    for (FlexibleStopPlace flexibleStopPlace : flexibleStopPlaces) {
      List<FlexibleArea> flexibleAreas = flexibleStopPlace
        .getAreas()
        .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
        .stream()
        .filter(FlexibleArea.class::isInstance)
        .map(FlexibleArea.class::cast)
        .toList();

      for (FlexibleArea flexibleArea : flexibleAreas) {
        AbstractRingType abstractRingType = flexibleArea
          .getPolygon()
          .getExterior()
          .getAbstractRing()
          .getValue();

        if (abstractRingType instanceof LinearRingType linearRingType) {
          List<Double> coordinates = getCoordinates(linearRingType);
          if (isProjectionValid(coordinates)) {
            Coordinate[] jtsCoordinates = createJTSCoordinates(coordinates);
            LinearRing linearRing = GeometryUtils
              .getGeometryFactory()
              .createLinearRing(jtsCoordinates);
            flexibleAreaContexts.add(
              new FlexibleAreaContext(
                flexibleStopPlace.getId(),
                flexibleArea.getId(),
                linearRing
              )
            );
          }
        }
      }
    }
    return flexibleAreaContexts;
  }

  private List<Double> getCoordinates(LinearRingType linearRing) {
    if (linearRing.getPosList() != null) {
      return linearRing
        .getPosList()
        .getValue();
    } else if (linearRing.getPosOrPointProperty() != null) {
      return linearRing
        .getPosOrPointProperty()
        .stream()
        .filter(DirectPositionType.class::isInstance)
        .map(DirectPositionType.class::cast)
        .map(DirectPositionType::getValue)
        .flatMap(Collection::stream)
        .toList();
    }
    LOGGER.debug(
      "LineString without posList or PosOrPointProperty: {}",
      linearRing
    );
    return Collections.emptyList();
  }

  private Coordinate[] createJTSCoordinates(
    List<Double> positionList
  ) {
    Coordinate[] coordinates = new Coordinate[positionList.size() / 2];
    for (int i = 0; i < positionList.size(); i += 2) {
      coordinates[i / 2] =
        new Coordinate(positionList.get(i + 1), positionList.get(i));
    }
    return coordinates;
  }

  /**
   * Validating that we have at least two coordinates (4 values) and the list size is even
   */
  private boolean isProjectionValid(List<Double> coordinates) {
    return coordinates.size() >= 4 && coordinates.size() % 2 == 0;
  }
}
