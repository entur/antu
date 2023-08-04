package no.entur.antu.validation.validator.interchange.mandatoryfields;

import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

public record MandatoryFieldsContext(
  String interchangeId,
  QuayId fromQuayId,
  QuayId toQuayId,
  ServiceJourney fromServiceJourney,
  ServiceJourney toServiceJourney
) {
  public static Builder builder(AntuNetexData antuNetexData) {
    return new Builder(antuNetexData);
  }

  public static class Builder {

    private final AntuNetexData antuNetexData;

    public Builder(AntuNetexData antuNetexData) {
      this.antuNetexData = antuNetexData;
    }

    public MandatoryFieldsContext build(
      ServiceJourneyInterchange serviceJourneyInterchange
    ) {
      return new MandatoryFieldsContext(
        serviceJourneyInterchange.getId(),
        antuNetexData.findQuayIdForScheduledStopPoint(
          ScheduledStopPointId.ofNullable(
            serviceJourneyInterchange.getFromPointRef()
          )
        ),
        antuNetexData.findQuayIdForScheduledStopPoint(
          ScheduledStopPointId.ofNullable(
            serviceJourneyInterchange.getToPointRef()
          )
        ),
        antuNetexData.serviceJourney(
          serviceJourneyInterchange.getFromJourneyRef()
        ),
        antuNetexData.serviceJourney(
          serviceJourneyInterchange.getToJourneyRef()
        )
      );
    }
  }
}
