package no.entur.antu.validation.validator.support;

import java.util.Map;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

public class JourneyPatternUtils {

  public static Map.Entry<ScheduledStopPointId, QuayCoordinates> coordinatesPerQuayId(
    StopPointInJourneyPattern stopPointInJourneyPattern,
    JAXBValidationContext validationContext
  ) {
    String scheduledStopPointRef = stopPointInJourneyPattern
      .getScheduledStopPointRef()
      .getValue()
      .getRef();

    if (scheduledStopPointRef == null) {
      return null;
    }

    ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
      scheduledStopPointRef
    );

    QuayId quayId = validationContext.quayIdForScheduledStopPoint(
      scheduledStopPointId
    );

    if (quayId == null) {
      return null;
    }

    QuayCoordinates coordinatesForQuayId = validationContext
      .getStopPlaceRepository()
      .getCoordinatesForQuayId(quayId);
    return coordinatesForQuayId == null
      ? null
      : Map.entry(scheduledStopPointId, coordinatesForQuayId);
  }
}
