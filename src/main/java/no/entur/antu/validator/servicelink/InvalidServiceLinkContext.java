package no.entur.antu.validator.servicelink;

import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.stop.StopPlaceRepository;
import org.locationtech.jts.geom.Coordinate;
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
      return commonDataRepository.findQuayIdForScheduledStopPoint(
        scheduledStopPointRef.getRef(),
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
        .filter(this::isProjectionValid)
        .map(this::createLineStringFromLineStringType)
        .filter(Objects::nonNull)
        .findFirst();
    }

    /**
     * Validating that we have at least two coordinates (4 values) and the list size is even
     */
    private boolean isProjectionValid(List<Double> coordinates) {
      return coordinates.size() >= 4 && coordinates.size() % 2 == 0;
    }

    private List<Double> getCoordinates(LineStringType lineString) {
      if (lineString.getPosList() != null) {
        return lineString.getPosList().getValue();
      } else if (lineString.getPosOrPointProperty() != null) {
        return lineString
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
        lineString.getId()
      );
      return Collections.emptyList();
    }

    private LineString createLineStringFromLineStringType(
      List<Double> positionList
    ) {
      Coordinate[] coordinates = new Coordinate[positionList.size() / 2];
      for (int i = 0; i < positionList.size(); i += 2) {
        coordinates[i / 2] =
          new Coordinate(positionList.get(i + 1), positionList.get(i));
      }
      return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }
  }
}