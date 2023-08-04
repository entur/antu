package no.entur.antu.validation.validator.servicelink;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.opengis.gml._3.LineStringType;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.utilities.GeometryUtilities;
import org.locationtech.jts.geom.LineString;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record InvalidServiceLinkContext(
  ServiceLink serviceLink,
  LineString lineString,
  QuayCoordinates from,
  QuayCoordinates to
) {
  public static final class Builder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private final String validationReportId;
    private final CommonDataRepository commonDataRepository;
    private final StopPlaceRepository stopPlaceRepository;

    public Builder(
      String validationReportId,
      CommonDataRepository commonDataRepository,
      StopPlaceRepository stopPlaceRepository
    ) {
      this.validationReportId = validationReportId;
      this.commonDataRepository = commonDataRepository;
      this.stopPlaceRepository = stopPlaceRepository;
    }

    public InvalidServiceLinkContext build(ServiceLink serviceLink) {
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
        LOGGER.debug(
          "Could not map serviceLink {} to a LineString",
          serviceLink.getId()
        );
        return null;
      }

      QuayCoordinates from = stopPlaceRepository.getCoordinatesForQuayId(
        fromQuayId
      );
      QuayCoordinates to = stopPlaceRepository.getCoordinatesForQuayId(
        toQuayId
      );

      if (from == null || to == null) {
        LOGGER.warn(
          "Could not find coordinates for either, from stopPoint or to stopPoint in serviceLink {}",
          serviceLink.getId()
        );
        return null;
      }

      return new InvalidServiceLinkContext(
        serviceLink,
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
      return commonDataRepository.findQuayIdForScheduledStopPoint(
        new ScheduledStopPointId(scheduledStopPointRef.getRef()),
        validationReportId
      );
    }

    private Optional<LineString> mapServiceLinkToLineString(
      ServiceLink serviceLink
    ) {
      if (
        serviceLink.getProjections() == null ||
        serviceLink.getProjections().getProjectionRefOrProjection() == null
      ) {
        return Optional.empty();
      }

      return serviceLink
        .getProjections()
        .getProjectionRefOrProjection()
        .stream()
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
