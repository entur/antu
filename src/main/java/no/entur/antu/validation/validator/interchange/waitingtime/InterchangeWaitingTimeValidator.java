package no.entur.antu.validation.validator.interchange.waitingtime;

import java.time.LocalDateTime;
import java.util.List;
import no.entur.antu.netexdata.DefaultNetexDataRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;

public class InterchangeWaitingTimeValidator extends AbstractDatasetValidator {

  static final ValidationRule RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE =
    new ValidationRule(
      "RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE",
      "Feeder and consumer vehicle journeys have no shared active dates",
      "ServiceJourneyInterchange %s has no shared date for feeder vehicle journey %s and consumer vehicle journey %s",
      Severity.ERROR
    );

  private final DefaultNetexDataRepository netexDataRepository;

  // TODO: Inject NetexDataRepository instead. Ensure serviceJourneyIdToActiveDates is added to the interface in validator lib
  protected InterchangeWaitingTimeValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    DefaultNetexDataRepository netexDataRepository
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  private List<LocalDateTime> getActiveDatesForServiceJourney(
    String validationReportId,
    ServiceJourneyId serviceJourneyId
  ) {
    return this.netexDataRepository.serviceJourneyIdToActiveDates(
        validationReportId
      )
      .get(serviceJourneyId.id());
  }

  private boolean serviceJourneysHaveSharedActiveDates(
    String validationReportId,
    ServiceJourneyId fromJourneyRef,
    ServiceJourneyId toJourneyRef,
    int dayOffsetDiff
  ) {
    // TODO: Is this too simple? Does it need to use day offsets aswell, as in Chouette?
    List<LocalDateTime> fromJourneyActiveDates =
      getActiveDatesForServiceJourney(validationReportId, fromJourneyRef);
    List<LocalDateTime> toJourneyActiveDates = getActiveDatesForServiceJourney(
      validationReportId,
      toJourneyRef
    );
    return fromJourneyActiveDates
      .stream()
      .anyMatch(toJourneyActiveDates::contains);
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    String validationReportId = validationReport.getValidationReportId();
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
      netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);

//    netexDataRepository.serviceJourneyStops(validationReportId).get(0).stream().forEach(entry -> {entry.});

    serviceJourneyInterchangeInfoList
      .stream()
      .filter(serviceJourneyInterchangeInfo -> {
        int arrivalDayOffset = netexDataRepository.serviceJourneyStops(validationReportId).get(serviceJourneyInterchangeInfo.fromJourneyRef()).stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId() == serviceJourneyInterchangeInfo.fromStopPoint()).findFirst().orElseThrow().arrivalDayOffset();
        int departureDayOffset = netexDataRepository.serviceJourneyStops(validationReportId).get(serviceJourneyInterchangeInfo.toJourneyRef()).stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId() == serviceJourneyInterchangeInfo.toStopPoint()).findFirst().orElseThrow().departureDayOffset();
        int dayOffsetDiff = departureDayOffset - arrivalDayOffset;

//          int dayOffsetDiff = consumerVJAtStop.getDepartureDayOffset() - feederVJAtStop.getArrivalDayOffset();
//          long msWait = TimeUtil.toMillisecondsOfDay(consumerVJAtStop.getDepartureTime())- TimeUtil.toMillisecondsOfDay(feederVJAtStop.getArrivalTime());
//          if (msWait < 0) {
//              msWait = DateUtils.MILLIS_PER_DAY + msWait;
//              dayOffsetDiff--;
//          }


            return !serviceJourneysHaveSharedActiveDates(
              validationReportId,
              serviceJourneyInterchangeInfo.fromJourneyRef(),
              serviceJourneyInterchangeInfo.toJourneyRef(),
                    dayOffsetDiff
            );
              }
      )
      .forEach(serviceJourneyInterchangeInfo -> {
        createValidationReportEntry(
          new ValidationIssue(
            RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE,
            new DataLocation(
              serviceJourneyInterchangeInfo.interchangeId(),
              serviceJourneyInterchangeInfo.filename(),
              null,
              null
            )
          )
        );
      });

    return validationReport;
  }
}
