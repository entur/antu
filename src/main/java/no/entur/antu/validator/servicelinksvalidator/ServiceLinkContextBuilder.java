package no.entur.antu.validator.servicelinksvalidator;

import net.opengis.gml._3.LineStringType;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceCoordinates;
import no.entur.antu.stop.StopPlaceRepository;
import org.locationtech.jts.geom.*;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBElement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ServiceLinkContextBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ServiceLinkContextBuilder.class
  );

  public record ServiceLinkContext(
    ServiceLink serviceLink,
    LineString lineString,
    StopPlaceCoordinates from,
    StopPlaceCoordinates to
  ) {
  }

  private final String validationReportId;
  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  public ServiceLinkContextBuilder(
    String validationReportId,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    this.validationReportId = validationReportId;
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
  }

  public ServiceLinkContext build(ServiceLink serviceLink) {
    QuayId fromQuayId = getQuayId(serviceLink.getFromPointRef());
    QuayId toQuayId = getQuayId(serviceLink.getToPointRef());

    if (fromQuayId == null || toQuayId == null) {
      LOGGER.debug(
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

    StopPlaceCoordinates from = stopPlaceRepository.getCoordinatesForQuayId(fromQuayId);
    StopPlaceCoordinates to = stopPlaceRepository.getCoordinatesForQuayId(toQuayId);

    return new ServiceLinkContext(serviceLink, lineString.get(), from, to);
  }

  public QuayId getQuayId(ScheduledStopPointRefStructure scheduledStopPointRef) {
    return commonDataRepository.findQuayIdForScheduledStopPoint(
      scheduledStopPointRef.getRef(),
      validationReportId
    );
  }

  private Optional<LineString> mapServiceLinkToLineString(ServiceLink serviceLink) {

    if (serviceLink.getProjections() == null ||
        serviceLink.getProjections().getProjectionRefOrProjection() == null) {
      return Optional.empty();
    }

    return serviceLink.getProjections().getProjectionRefOrProjection().stream()
      .map(JAXBElement::getValue)
      .filter(LinkSequenceProjection_VersionStructure.class::isInstance)
      .map(LinkSequenceProjection_VersionStructure.class::cast)
      .map(LinkSequenceProjection_VersionStructure::getLineString)
      .filter(this::isProjectionValid)
      .map(this::createLineStringFromLineStringType)
      .filter(Objects::nonNull)
      .findFirst();
  }

  private boolean isProjectionValid(LineStringType lineString) {
    if (lineString == null) {
      return false;
    }

    // Assuming getPosList() and getValue() are guaranteed to be non-null.
    List<Double> coordinates = lineString.getPosList().getValue();

    // Validating that we have at least two coordinates, i.e. start and end point
    if (coordinates.size() < 4) {
      return false;
    }

    // Check if the coordinates list size is even
    // Validating that we have both longitude and latitude for each coordinate
    return coordinates.size() % 2 == 0;
  }

  private LineString createLineStringFromLineStringType(LineStringType lineStringType) {
    List<Double> positionList = lineStringType.getPosList().getValue();
    Coordinate[] coordinates = new Coordinate[positionList.size() / 2];
    for (int i = 0; i < positionList.size(); i += 2) {
      coordinates[i / 2] = new Coordinate(positionList.get(i + 1), positionList.get(i));
    }
    return GeometryUtils.getGeometryFactory().createLineString(coordinates);
  }
}
