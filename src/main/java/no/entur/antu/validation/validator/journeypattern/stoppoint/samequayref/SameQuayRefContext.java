package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import java.util.List;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.JourneyPattern;

public record SameQuayRefContext(
  String journeyPatternId,
  ScheduledStopPointId scheduledStopPointId,
  QuayId quayId
) {
  public boolean isValid() {
    return scheduledStopPointId != null && quayId != null;
  }

  public static Builder builder(JAXBValidationContext validationContext) {
    return new Builder(validationContext);
  }

  public static class Builder {

    private final JAXBValidationContext validationContext;

    private Builder(JAXBValidationContext validationContext) {
      this.validationContext = validationContext;
    }

    public List<SameQuayRefContext> build(JourneyPattern journeyPattern) {
      return NetexUtils
        .stopPointsInJourneyPattern(journeyPattern)
        .stream()
        .map(ScheduledStopPointId::of)
        .map(stopPointId ->
          new SameQuayRefContext(
            journeyPattern.getId(),
            stopPointId,
            validationContext.quayIdForScheduledStopPoint(stopPointId)
          )
        )
        .toList();
    }
  }
}
