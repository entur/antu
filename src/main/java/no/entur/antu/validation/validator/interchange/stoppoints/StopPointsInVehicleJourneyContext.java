package no.entur.antu.validation.validator.interchange.stoppoints;

import java.util.List;
import java.util.Map;
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
  public static class Builder {

    private final String validationReportId;
    private final NetexDataRepository netexDataRepository;
    private Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyIdListMap;

    public Builder(
      String validationReportId,
      NetexDataRepository netexDataRepository
    ) {
      this.validationReportId = validationReportId;
      this.netexDataRepository = netexDataRepository;
    }

    public Builder primeCache() {
      serviceJourneyIdListMap =
        netexDataRepository.serviceJourneyStops(validationReportId);
      return this;
    }

    public StopPointsInVehicleJourneyContext build(
      ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo
    ) {
      return new StopPointsInVehicleJourneyContext(
        serviceJourneyInterchangeInfo,
        Optional
          .ofNullable(serviceJourneyInterchangeInfo.fromJourneyRef())
          .map(serviceJourneyId -> serviceJourneyIdListMap.get(serviceJourneyId)
          )
          .orElse(null),
        Optional
          .ofNullable(serviceJourneyInterchangeInfo.toJourneyRef())
          .map(serviceJourneyId -> serviceJourneyIdListMap.get(serviceJourneyId)
          )
          .orElse(null)
      );
    }
  }

  public boolean isValid() {
    return (
      serviceJourneyInterchangeInfo != null &&
      serviceJourneyInterchangeInfo.isValid()
    );
  }
}
