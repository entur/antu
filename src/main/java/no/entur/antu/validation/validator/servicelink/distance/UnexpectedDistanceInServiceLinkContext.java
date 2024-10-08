package no.entur.antu.validation.validator.servicelink.distance;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.opengis.gml._3.LineStringType;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.utilities.GeometryUtilities;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.locationtech.jts.geom.LineString;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.Projections_RelStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record UnexpectedDistanceInServiceLinkContext(
  String serviceLinkId,
  ScheduledStopPointId fromScheduledStopPointId,
  ScheduledStopPointId toScheduledStopPointId,
  LineString lineString,
  QuayCoordinates fromQuayCoordinates,
  QuayCoordinates toQuayCoordinates
) {
  public static final class Builder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private final AntuNetexData antuNetexData;

    public Builder(AntuNetexData antuNetexData) {
      this.antuNetexData = antuNetexData;
    }

    public UnexpectedDistanceInServiceLinkContext build(
      ServiceLink serviceLink
    ) {
      QuayId fromQuayId = getQuayId(serviceLink.getFromPointRef());
      QuayId toQuayId = getQuayId(serviceLink.getToPointRef());

      if (fromQuayId == null || toQuayId == null) {
        LOGGER.warn(
          "Could not find quayId for either, from stopPoint or to stopPoint in serviceLink {}",
          serviceLink.getId()
        );
        return null;
      }

      Optional<LineString> lineString = mapServiceLinkToLineString(serviceLink);

      if (lineString.isEmpty()) {
        LOGGER.warn(
          "Could not map serviceLink {} to a LineString",
          serviceLink.getId()
        );
        return null;
      }

      QuayCoordinates from = antuNetexData.coordinatesForQuayId(fromQuayId);
      QuayCoordinates to = antuNetexData.coordinatesForQuayId(toQuayId);

      if (from == null || to == null) {
        LOGGER.warn(
          "Could not find coordinates for either, from stopPoint or to stopPoint in serviceLink {}",
          serviceLink.getId()
        );
        return null;
      }

      return new UnexpectedDistanceInServiceLinkContext(
        serviceLink.getId(),
        new ScheduledStopPointId(serviceLink.getFromPointRef().getRef()),
        new ScheduledStopPointId(serviceLink.getToPointRef().getRef()),
        lineString.get(),
        from,
        to
      );
    }

    public QuayId getQuayId(
      ScheduledStopPointRefStructure scheduledStopPointRef
    ) {
      // Q: Hva med StopPlaceAssignments are in the Line file?
      // A: We can only from Line file to Common file, common file
      // cannot refer to the line file i.e. the service Links in the
      // common file cannot refer to the Stop Place assignments in the
      // line file.
      return Optional
        .ofNullable(scheduledStopPointRef)
        .map(VersionOfObjectRefStructure::getRef)
        .map(ScheduledStopPointId::new)
        .map(antuNetexData::quayIdForScheduledStopPoint)
        .orElse(null);
    }

    private Optional<LineString> mapServiceLinkToLineString(
      ServiceLink serviceLink
    ) {
      return Optional
        .ofNullable(serviceLink.getProjections())
        .map(Projections_RelStructure::getProjectionRefOrProjection)
        .stream()
        .flatMap(List::stream)
        .map(JAXBElement::getValue)
        .filter(LinkSequenceProjection_VersionStructure.class::isInstance)
        .map(LinkSequenceProjection_VersionStructure.class::cast)
        .map(LinkSequenceProjection_VersionStructure::getLineString)
        .filter(Objects::nonNull)
        .map(this::getCoordinates)
        .filter(GeometryUtilities::isValidCoordinatesList)
        .map(GeometryUtilities::createLineStringFromLineStringType)
        .filter(Objects::nonNull)
        .findFirst();
    }

    private List<Double> getCoordinates(LineStringType lineString) {
      List<Double> coordinates = GeometryUtilities.getCoordinates(lineString);
      if (coordinates.isEmpty()) {
        LOGGER.warn("LineString with no coordinates: {}", lineString.getId());
      }
      return coordinates;
    }
  }
}
