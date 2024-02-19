package no.entur.antu.flex.validation.validator.flexibleareavalidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.opengis.gml._3.AbstractRingType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LinearRingType;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.FlexibleStopPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.Site_VersionFrameStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexibleAreaContextBuilder {

  public record FlexibleAreaContext(
    String flexibleStopPlaceId,
    String flexibleAreaId,
    List<Double> coordinates
  ) {

    public Coordinate[] getJTSCoordinates() {
      Coordinate[] jtsCoordinates = new Coordinate[coordinates.size() / 2];
      for (int i = 0; i < coordinates.size(); i += 2) {
        jtsCoordinates[i / 2] =
          new Coordinate(coordinates.get(i + 1), coordinates.get(i));
      }
      return jtsCoordinates;
    }

    /**
     * Validating that we have at least two coordinates (4 values) and the list size is even
     */
    public boolean isEvenCoordinates() {
      return coordinates.size() % 2 == 0;
    }
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(
    FlexibleAreaContextBuilder.class
  );

  public List<FlexibleAreaContext> build(
    NetexEntitiesIndex index
  ) {
    List<FlexibleAreaContext> flexibleAreaContexts = new ArrayList<>();

    List<FlexibleStopPlace> flexibleStopPlaces = index
      .getSiteFrames()
      .stream()
      .map(Site_VersionFrameStructure::getFlexibleStopPlaces)
      .filter(Objects::nonNull)
      .map(FlexibleStopPlacesInFrame_RelStructure::getFlexibleStopPlace)
      .flatMap(List::stream)
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
          flexibleAreaContexts.add(
            new FlexibleAreaContext(
              flexibleStopPlace.getId(),
              flexibleArea.getId(),
              getCoordinates(linearRingType)
            )
          );
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
}
