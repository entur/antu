package no.entur.antu.validation.flex.validator.flexiblearea;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.AbstractRingType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import no.entur.antu.validation.utilities.GeometryUtilities;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record InvalidFlexibleAreaContext(
  String flexibleStopPlaceId,
  String flexibleAreaId,
  List<Double> coordinates
) {
  private static final Logger LOGGER = LoggerFactory.getLogger(
    InvalidFlexibleAreaContext.class
  );

  public static List<InvalidFlexibleAreaContext> of(
    FlexibleStopPlace flexibleStopPlace
  ) {
    List<InvalidFlexibleAreaContext> flexibleAreaContexts = flexibleStopPlace
      .getAreas()
      .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
      .stream()
      .filter(FlexibleArea.class::isInstance)
      .map(FlexibleArea.class::cast)
      .map(flexibleArea -> {
        AbstractRingType abstractRingType = getAbstractRingType(flexibleArea);

        if (abstractRingType instanceof LinearRingType linearRingType) {
          return new InvalidFlexibleAreaContext(
            flexibleStopPlace.getId(),
            flexibleArea.getId(),
            getCoordinates(linearRingType)
          );
        } else {
          LOGGER.warn(
            "Invalid flexible area: {}, skipping the validation.",
            flexibleArea.getId()
          );
          return null;
        }
      })
      .filter(Objects::nonNull)
      .toList();

    return flexibleAreaContexts;
  }

  public boolean hasValidCoordinates() {
    return GeometryUtilities.isValidCoordinatesList(coordinates);
  }

  private static AbstractRingType getAbstractRingType(
    FlexibleArea flexibleArea
  ) {
    return Optional
      .ofNullable(flexibleArea.getPolygon())
      .map(PolygonType::getExterior)
      .map(AbstractRingPropertyType::getAbstractRing)
      .map(JAXBElement::getValue)
      .orElse(null);
  }

  private static List<Double> getCoordinates(LinearRingType linearRing) {
    List<Double> coordinates = GeometryUtilities.getCoordinates(linearRing);
    if (coordinates.isEmpty()) {
      LOGGER.warn("LinearRing with no coordinates found");
    }
    return coordinates;
  }
}
