package no.entur.antu.validation.validator.interchange.mandatoryfields;

import java.util.Optional;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

public record MandatoryFieldsContext(
  String interchangeId,
  String fromPointRef,
  String toPointRef,
  String fromJourneyRef,
  String toJourneyRef
) {
  public static MandatoryFieldsContext of(
    ServiceJourneyInterchange serviceJourneyInterchange
  ) {
    return new MandatoryFieldsContext(
      serviceJourneyInterchange.getId(),
      Optional
        .ofNullable(serviceJourneyInterchange.getFromPointRef())
        .map(VersionOfObjectRefStructure::getRef)
        .orElse(null),
      Optional
        .ofNullable(serviceJourneyInterchange.getToPointRef())
        .map(VersionOfObjectRefStructure::getRef)
        .orElse(null),
      Optional
        .ofNullable(serviceJourneyInterchange.getFromJourneyRef())
        .map(VersionOfObjectRefStructure::getRef)
        .orElse(null),
      Optional
        .ofNullable(serviceJourneyInterchange.getToJourneyRef())
        .map(VersionOfObjectRefStructure::getRef)
        .orElse(null)
    );
  }
}
