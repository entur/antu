package no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.JourneyPattern;

public record IdenticalStopPointsContext(
  String journeyPatternId,
  List<StopPointContext> stopPointContexts
) {
  public record StopPointContext(
    QuayId quayId,
    String destinationDisplayRef,
    Boolean forBoarding,
    Boolean forAlighting
  ) {}

  public static Builder builder(JAXBValidationContext validationContext) {
    return new Builder(validationContext);
  }

  /**
   * The equals method is used by the groupingBy collector in the validate method.
   *
   * @param obj the reference object with which to compare.
   * @return true if the two lists of pointRefs are equal, false otherwise.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IdenticalStopPointsContext that) {
      return this.stopPointContexts().equals(that.stopPointContexts());
    }
    return false;
  }

  /**
   * The hashCode method is used by the groupingBy collector in the validate method.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return stopPointContexts.hashCode();
  }

  public static class Builder {

    private final JAXBValidationContext validationContext;

    private Builder(JAXBValidationContext validationContext) {
      this.validationContext = validationContext;
    }

    public IdenticalStopPointsContext build(JourneyPattern journeyPattern) {
      return new IdenticalStopPointsContext(
        journeyPattern.getId(),
        NetexUtils
          .stopPointsInJourneyPattern(journeyPattern)
          .stream()
          .filter(Objects::nonNull)
          .map(stopPoint ->
            new StopPointContext(
              validationContext.quayIdForScheduledStopPoint(
                ScheduledStopPointId.of(stopPoint)
              ),
              Optional
                .ofNullable(stopPoint.getDestinationDisplayRef())
                .map(DestinationDisplayRefStructure::getRef)
                .orElse(null),
              stopPoint.isForBoarding(),
              stopPoint.isForAlighting()
            )
          )
          .toList()
      );
    }
  }
}
