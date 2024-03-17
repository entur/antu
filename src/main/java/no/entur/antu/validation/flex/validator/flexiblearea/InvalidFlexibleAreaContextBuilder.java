package no.entur.antu.validation.flex.validator.flexiblearea;

import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.AbstractRingType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import no.entur.antu.validation.utilities.GeometryUtilities;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.FlexibleStopPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.Site_VersionFrameStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvalidFlexibleAreaContextBuilder {

  public record InvalidFlexibleAreaContext(
    String flexibleStopPlaceId,
    String flexibleAreaId,
    List<Double> coordinates
  ) {}

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InvalidFlexibleAreaContextBuilder.class
  );

  public List<InvalidFlexibleAreaContext> build(NetexEntitiesIndex index) {
    List<InvalidFlexibleAreaContext> invalidFlexibleAreaContexts =
      new ArrayList<>();

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
        AbstractRingType abstractRingType = getAbstractRingType(flexibleArea);

        if (abstractRingType instanceof LinearRingType linearRingType) {
          invalidFlexibleAreaContexts.add(
            new InvalidFlexibleAreaContext(
              flexibleStopPlace.getId(),
              flexibleArea.getId(),
              getCoordinates(linearRingType)
            )
          );
        } else {
          LOGGER.warn(
            "Invalid flexible area: {}, skipping the validation.",
            flexibleArea.getId()
          );
        }
      }
    }
    return invalidFlexibleAreaContexts;
  }

  private AbstractRingType getAbstractRingType(FlexibleArea flexibleArea) {
    return Optional
      .ofNullable(flexibleArea.getPolygon())
      .map(PolygonType::getExterior)
      .map(AbstractRingPropertyType::getAbstractRing)
      .map(JAXBElement::getValue)
      .orElse(null);
  }

  private List<Double> getCoordinates(LinearRingType linearRing) {
    List<Double> coordinates = GeometryUtilities.getCoordinates(linearRing);
    if (coordinates.isEmpty()) {
      LOGGER.warn("LinearRing with no coordinates found");
    }
    return coordinates;
  }
}
