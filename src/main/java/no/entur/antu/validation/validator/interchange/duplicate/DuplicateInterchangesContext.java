package no.entur.antu.validation.validator.interchange.duplicate;

import java.util.Objects;
import java.util.Optional;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

public record DuplicateInterchangesContext(
  String interchangeId,
  InterchangeContext interchangeContext
) {
  public record InterchangeContext(
    ScheduledStopPointId FromPointRef,
    ScheduledStopPointId ToPointRef,
    String FromJourneyRef,
    String ToJourneyRef
  ) {}

  public static DuplicateInterchangesContext of(
    ServiceJourneyInterchange serviceJourneyInterchange
  ) {
    return new DuplicateInterchangesContext(
      serviceJourneyInterchange.getId(),
      new InterchangeContext(
        ScheduledStopPointId.ofNullable(
          serviceJourneyInterchange.getFromPointRef()
        ),
        ScheduledStopPointId.ofNullable(
          serviceJourneyInterchange.getToPointRef()
        ),
        Optional
          .ofNullable(serviceJourneyInterchange.getFromJourneyRef())
          .map(VehicleJourneyRefStructure::getRef)
          .orElse(null),
        Optional
          .ofNullable(serviceJourneyInterchange.getToJourneyRef())
          .map(VehicleJourneyRefStructure::getRef)
          .orElse(null)
      )
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof DuplicateInterchangesContext that)) return false;
    return Objects.equals(interchangeContext, that.interchangeContext);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(interchangeContext);
  }
}
