package no.entur.antu.validation.validator.support;

import java.util.Map;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JourneyPatternUtils {

  static Logger log = LoggerFactory.getLogger(JourneyPatternUtils.class);

  private JourneyPatternUtils() {}

  public static Map.Entry<ScheduledStopPointId, QuayCoordinates> coordinatesPerQuayId(
    StopPointInJourneyPattern stopPointInJourneyPattern,
    JAXBValidationContext validationContext
  ) {
    String scheduledStopPointRef = stopPointInJourneyPattern
      .getScheduledStopPointRef()
      .getValue()
      .getRef();

    if (scheduledStopPointRef == null) {
      log.warn(
        "Scheduled stop point ref is null for stop point {}",
        stopPointInJourneyPattern.getId()
      );
      return null;
    }

    ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
      scheduledStopPointRef
    );

    QuayId quayId = validationContext.quayIdForScheduledStopPoint(
      scheduledStopPointId
    );

    if (quayId == null) {
      log.warn(
        "Quay ID not found for scheduled stop point id {}",
        scheduledStopPointId
      );
      return null;
    }

    QuayCoordinates coordinatesForQuayId =
      validationContext.coordinatesForQuayId(quayId);

    if (coordinatesForQuayId == null) {
      log.warn("Quay coordinates not found for quay id {}", quayId);
      return null;
    }

    return Map.entry(scheduledStopPointId, coordinatesForQuayId);
  }
}
