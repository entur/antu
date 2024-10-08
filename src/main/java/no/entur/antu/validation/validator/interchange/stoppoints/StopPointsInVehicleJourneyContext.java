package no.entur.antu.validation.validator.interchange.stoppoints;

import java.util.List;
import java.util.Optional;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;

public record StopPointsInVehicleJourneyContext(
  ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
  List<ServiceJourneyStop> serviceJourneyStopsForFromJourneyRef,
  List<ServiceJourneyStop> serviceJourneyStopsForToJourneyRef
) {
  public static StopPointsInVehicleJourneyContext of(
    String validationReportId,
    NetexDataRepository netexDataRepository,
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo
  ) {
    return new StopPointsInVehicleJourneyContext(
      serviceJourneyInterchangeInfo,
      Optional
        .ofNullable(serviceJourneyInterchangeInfo.fromJourneyRef())
        .map(serviceJourneyId ->
          serviceJourneyStops(
            validationReportId,
            netexDataRepository,
            serviceJourneyId
          )
        )
        .orElse(null),
      Optional
        .ofNullable(serviceJourneyInterchangeInfo.toJourneyRef())
        .map(serviceJourneyId ->
          serviceJourneyStops(
            validationReportId,
            netexDataRepository,
            serviceJourneyId
          )
        )
        .orElse(null)
    );
  }

  public boolean isValid() {
    return (
      serviceJourneyInterchangeInfo != null &&
      serviceJourneyInterchangeInfo.isValid() &&
      serviceJourneyStopsForFromJourneyRef != null &&
      serviceJourneyStopsForToJourneyRef != null
    );
  }

  private static List<ServiceJourneyStop> serviceJourneyStops(
    String validationReportId,
    NetexDataRepository netexDataRepository,
    ServiceJourneyId serviceJourneyId
  ) {
    return Optional
      .ofNullable(
        netexDataRepository.serviceJourneyStops(
          validationReportId,
          serviceJourneyId
        )
      )
      .orElse(List.of());
  }
}
