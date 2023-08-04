package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.stoppointinjourneypatternvalidator.StopPointInJourneyPatternContextBuilder.StopPointInJourneyPatternContext;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopPointInJourneyPatternValidator extends AbstractNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPointInJourneyPatternValidator.class
  );
  private static final String MISSING_SCHEDULED_STOP_ASSIGNMENT =
    "MISSING_SCHEDULED_STOP_ASSIGNMENT";
  private static final String MISSING_SCHEDULED_STOP_ASSIGNMENT_DESCRIPTION =
    "Missing ScheduledStopAssignment for StopPointInJourneyPattern, while the ServiceJourney exists.";
  private final CommonDataRepository commonDataRepository;

  public StopPointInJourneyPatternValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository
  ) {
    super(validationReportEntryFactory);
    this.commonDataRepository = commonDataRepository;
  }

  private boolean isAssignedToDeadRun(
    List<DeadRun> deadRuns,
    String journeyPatternRef
  ) {
    return deadRuns
      .stream()
      .map(deadRun -> deadRun.getJourneyPatternRef().getValue().getRef())
      .anyMatch(deadRunJourneyPatternRef ->
        deadRunJourneyPatternRef.equals(journeyPatternRef)
      );
  }

  private boolean isNotAssignedToAnyServiceJourney(
    List<ServiceJourney> serviceJourneys,
    String journeyPatternRef
  ) {
    return serviceJourneys
      .stream()
      .map(serviceJourney ->
        serviceJourney.getJourneyPatternRef().getValue().getRef()
      )
      .noneMatch(serviceJourneyJourneyPatternRef ->
        serviceJourneyJourneyPatternRef.equals(journeyPatternRef)
      );
  }

  private boolean validateStopPointInJourneyPattern(
    NetexEntitiesIndex index,
    StopPointInJourneyPatternContext stopPointInJourneyPatternContext
  ) {
    Map<Boolean, List<Journey_VersionStructure>> deadRunsAndRestOfServiceJourneys =
      index
        .getTimetableFrames()
        .stream()
        .flatMap(timetableFrame ->
          timetableFrame
            .getVehicleJourneys()
            .getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney()
            .stream()
        )
        .collect(Collectors.partitioningBy(DeadRun.class::isInstance));

    List<DeadRun> deadRuns = deadRunsAndRestOfServiceJourneys
      .get(Boolean.TRUE)
      .stream()
      .filter(DeadRun.class::isInstance) // Don't need it
      .map(DeadRun.class::cast)
      .toList();

    List<ServiceJourney> serviceJourneys = deadRunsAndRestOfServiceJourneys
      .get(Boolean.FALSE)
      .stream()
      .filter(ServiceJourney.class::isInstance)
      .map(ServiceJourney.class::cast)
      .toList();

    /*
        Missing SPA -> No DeadRun -> No SJ -> Error
        Missing SPA -> No DeadRun -> Yes SJ -> Error
        Missing SPA -> Yes DeadRun -> Yes SJ -> Error
        Missing SPA -> Yes DeadRun -> No SJ -> OK
         */
    return (
      isAssignedToDeadRun(
        deadRuns,
        stopPointInJourneyPatternContext.journeyPattern().getId()
      ) &&
      isNotAssignedToAnyServiceJourney(
        serviceJourneys,
        stopPointInJourneyPatternContext.journeyPattern().getId()
      )
    );
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating Stop place in journey pattern");

    if (validationContext.isCommonFile()) {
      return;
    }

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex index =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();

      StopPointInJourneyPatternContextBuilder builder =
        new StopPointInJourneyPatternContextBuilder(
          validationReport.getValidationReportId(),
          commonDataRepository,
          index
        );
      List<StopPointInJourneyPatternContext> stopPointInJourneyPatternContexts =
        index
          .getJourneyPatternIndex()
          .getAll()
          .stream()
          .flatMap(journeyPattern -> builder.build(journeyPattern).stream())
          .toList();

      Predicate<StopPointInJourneyPatternContext> hasNoStopPointAssignmentInCommonFile =
        stopPointInJourneyPatternContext ->
          commonDataRepository.findQuayIdForScheduledStopPoint(
            stopPointInJourneyPatternContext
              .stopPointInJourneyPattern()
              .getScheduledStopPointRef()
              .getValue()
              .getRef(),
            validationReport.getValidationReportId()
          ) ==
          null;

      Predicate<StopPointInJourneyPatternContext> hasNoStopPointAssignmentInLineFile =
        stopPointInJourneyPatternContext ->
          !index
            .getPassengerStopAssignmentsByStopPointRefIndex()
            .containsKey(
              stopPointInJourneyPatternContext
                .stopPointInJourneyPattern()
                .getScheduledStopPointRef()
                .getValue()
                .getRef()
            );

      stopPointInJourneyPatternContexts
        .stream()
        .filter(
          hasNoStopPointAssignmentInCommonFile.and(
            hasNoStopPointAssignmentInLineFile
          )
        )
        .filter(stopPointInJourneyPatternContext ->
          !validateStopPointInJourneyPattern(
            index,
            stopPointInJourneyPatternContext
          )
        )
        .map(StopPointInJourneyPatternContext::stopPointInJourneyPattern)
        .forEach(stopPointInJourneyPattern ->
          addValidationReportEntry(
            validationReport,
            validationContext,
            stopPointInJourneyPattern
          )
        );
    } else {
      throw new AntuException(
        "Received invalid validation context in Stop Point Validation"
      );
    }
  }

  private void addValidationReportEntry(
    ValidationReport validationReport,
    ValidationContext validationContext,
    StopPointInJourneyPattern stopPointInJourneyPattern
  ) {
    ValidationReportEntry validationReportEntry = createValidationReportEntry(
      MISSING_SCHEDULED_STOP_ASSIGNMENT,
      findDataLocation(validationContext, stopPointInJourneyPattern),
      MISSING_SCHEDULED_STOP_ASSIGNMENT_DESCRIPTION
    );

    validationReport.addValidationReportEntry(validationReportEntry);
  }

  private static DataLocation findDataLocation(
    ValidationContext validationContext,
    StopPointInJourneyPattern stopPointInJourneyPattern
  ) {
    String fileName = validationContext.getFileName();
    return validationContext
      .getLocalIds()
      .stream()
      .filter(localId ->
        localId.getId().equals(stopPointInJourneyPattern.getId())
      )
      .findFirst()
      .map(idVersion ->
        new DataLocation(
          idVersion.getId(),
          fileName,
          idVersion.getLineNumber(),
          idVersion.getColumnNumber()
        )
      )
      .orElse(
        new DataLocation(stopPointInJourneyPattern.getId(), fileName, 0, 0)
      );
  }

  @Override
  public Set<String> getRuleDescriptions() {
    return Set.of(
      createRuleDescription(
        MISSING_SCHEDULED_STOP_ASSIGNMENT,
        MISSING_SCHEDULED_STOP_ASSIGNMENT_DESCRIPTION
      )
    );
  }
}
