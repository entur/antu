package no.entur.antu.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import no.entur.antu.exception.AntuException;
import no.entur.antu.utils.zip.ZipStreamUtil;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntityIndex;
import org.entur.netex.validation.validator.NetexValidationProgressCallBack;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReport;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate a NeTEx dataset according to a given validation profile.
 * A Validation profile defines the set of rules to be applied during the validation.
 */
public class NetexValidationProfile {

  private static final Logger log = LoggerFactory.getLogger(NetexValidationProfile.class);
  private final Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners;
  private final boolean skipSchemaValidation;
  private final boolean skipNetexValidators;

  public NetexValidationProfile(
    Map<ValidationProfile, NetexValidatorsRunner> netexValidatorsRunners,
    boolean skipSchemaValidation,
    boolean skipNetexValidators
  ) {
    this.netexValidatorsRunners = netexValidatorsRunners;
    this.skipSchemaValidation = skipSchemaValidation;
    this.skipNetexValidators = skipNetexValidators;
  }

  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @param validationProfile  the NeTEx validation profile
   * @param codespace          the dataset codespace.
   * @param validationReportId the report id.
   * @param filename           the name of the NeTEx file.
   * @param fileContent        the binary content of the NeTEx file.
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport validate(
    String validationProfile,
    String codespace,
    String validationReportId,
    String filename,
    byte[] fileContent,
    NetexValidationProgressCallBack netexValidationProgressCallBack
  ) {
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }
    if (codespace == null) {
      throw new AntuException("Missing codespace");
    }
    NetexValidatorsRunner netexValidatorsRunner = getNetexValidatorsRunner(
      validationProfile
    );
    return netexValidatorsRunner.validate(
      codespace,
      validationReportId,
      filename,
      fileContent,
      skipSchemaValidation,
      skipNetexValidators,
      netexValidationProgressCallBack
    );
  }

  private Stream<InputStream> zipFileToStreamOfInputStreams(InputStream zipFileAsInputStream) {
    try {
      Stream<InputStream> extractedStreams =
              ZipStreamUtil.createInputStreamFromZip(zipFileAsInputStream);
      log.info(
              "Successfully prepared ZIP stream for XML processing"
      );
      return extractedStreams;
    } catch (Exception e) {
      log.error("Exception during file extraction" + e);
      throw e;
    }
  }

  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport crossValidateNetexDataset(InputStream zipFileInputStream, String validationProfile) throws IOException {
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }


    log.info("Validating netex dataset...");

    NetexParser netexParser = new NetexParser();
    byte[] zipAsByteArray = zipFileInputStream.readAllBytes();
    Set<ServiceJourneyInterChangeValidationContext> validationContexts = new HashSet<>();

    Map<String, ServiceJourneyInterChangeValidationContext> toServiceJourneyIdsToContext = new HashMap<>();
    Map<String, ServiceJourneyInterChangeValidationContext> fromServiceJourneyIdsToContext = new HashMap<>();
    Map<String, ServiceJourneyInterChangeValidationContext> scheduledStopPointIdsToContext = new HashMap<>();

    var iterator = zipFileToStreamOfInputStreams(new ByteArrayInputStream(zipAsByteArray)).iterator();
    while (iterator.hasNext()) {
      InputStream fileAsInputStream = iterator.next();
      NetexEntitiesIndex index = netexParser.parse(fileAsInputStream);
      for (ServiceJourneyInterchange interchange : index.getServiceJourneyInterchangeIndex().getAll()) {
        var validationContext = new ServiceJourneyInterChangeValidationContext(
            interchange.getId(),
            interchange.getFromJourneyRef().getRef(),
            interchange.getToJourneyRef().getRef(),
            interchange.getFromPointRef().getRef(),
            interchange.getToPointRef().getRef(),
            interchange.getMaximumWaitTime()
        );
        fromServiceJourneyIdsToContext.put(interchange.getFromJourneyRef().getRef(), validationContext);
        toServiceJourneyIdsToContext.put(interchange.getToJourneyRef().getRef(), validationContext);
        scheduledStopPointIdsToContext.put(interchange.getFromPointRef().getRef(), validationContext);
        scheduledStopPointIdsToContext.put(interchange.getToPointRef().getRef(), validationContext);
        validationContexts.add(validationContext);
      }
    }

    Map<String, ServiceJourney> mapOfServiceJourneys = new HashMap<>();
    iterator = zipFileToStreamOfInputStreams(new ByteArrayInputStream(zipAsByteArray)).iterator();
    /*
     *
     * Fra ServiceJourneyInterchange trenger vi:
     * 1. ServiceJourneyRefene til de to servicejourneyene
     * 2. ScheduledStopPointRefene til de to ScheduledStopPointene
     * 3. MaximumWaitTime
     *
     * Fra journeypattern trenger vi:
     * 1. StopPointInJourneyPattern IDer som har ScheduledStopPointRefene fra ServiceJourneyInterchange
     *
     * Fra servicejourney trenger vi:
     * 1. ID
     * 2. DepartureTime og ArrivalTime, DepartureDayOffset og ArrivalDayOffset fra de TimetabledPassingTime entitetene som har StopPointInJourneyPatternRefene fra journeypattern
     * 3. DayTypeRef
     *
     *
     * ScheduledStopPoint ID er referert til i ServiceJourneyInterchange,
     * JourneyPattern har ScheduledStopPointRef i en StopPointInJourneyPattern entitet
     * StopPointInJourneyPattern har en ID (eg. SKY:StopPointInJourneyPattern:311-468-1)
     * StopPointInJourneyPattern IDen er referert til i StopPointInJourneyPatternRef inne i en TimetabledPassingTime
     * TimetabledPassingTime er inne i en ServiceJourney
     * TimetabledPassingTime har også en departure time som vi må bruke
     *
     *
     * SKY:ScheduledStopPoint:default-58351
     */
    Set<String> stopPointInJourneyPatternIds = new HashSet<>();
    while (iterator.hasNext()) {
      InputStream fileInputStream = iterator.next();
      NetexEntitiesIndex index = netexParser.parse(fileInputStream);

      for (JourneyPattern journeyPattern : index.getJourneyPatternIndex().getAll()) {
        List<StopPointInJourneyPattern> stopPointInJourneyPatterns = journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().stream()
            .filter(pointInJourneyPattern -> pointInJourneyPattern instanceof StopPointInJourneyPattern)
            .map(point -> (StopPointInJourneyPattern) point)
            .toList();

        for (StopPointInJourneyPattern stopPointInJourneyPattern : stopPointInJourneyPatterns) {
          if (stopPointInJourneyPattern.getScheduledStopPointRef() != null && scheduledStopPointIdsToContext.containsKey(stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef())) {
            var validationContext = scheduledStopPointIdsToContext.get(stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef());
            validationContext.addStopPointInJourneyPatternRef(stopPointInJourneyPattern.getId());
            stopPointInJourneyPatternIds.add(stopPointInJourneyPattern.getId());
          }
        }
      }
    }

    iterator = zipFileToStreamOfInputStreams(new ByteArrayInputStream(zipAsByteArray)).iterator();
    Set<TimetabledPassingTime> timetabledPassingTimes = new HashSet<>();
    while (iterator.hasNext()) {
      InputStream fileInputStream = iterator.next();
      NetexEntitiesIndex index = netexParser.parse(fileInputStream);

      for (ServiceJourney serviceJourney : index.getServiceJourneyIndex().getAll()) {
        if (serviceJourneyIds.contains(serviceJourney.getId())) {
          mapOfServiceJourneys.put(serviceJourney.getId(), serviceJourney);
          serviceJourneyIds.remove(serviceJourney.getId());
        }

        for (TimetabledPassingTime timetabledPassingTime : serviceJourney.getPassingTimes().getTimetabledPassingTime()) {
          if (stopPointInJourneyPatternIds.contains(timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef())) {
            timetabledPassingTimes.add(timetabledPassingTime);
          }
        }
      }
    }
    return new ValidationReport();
  }


  /**
   * Validate a NeTEx file according to a validation profile
   *
   * @return a ValidationReport listing the findings for this NeTEx file.
   */
  public ValidationReport validateDataset(
    ValidationReport validationReport,
    String validationProfile,
    NetexValidationProgressCallBack netexValidationProgressCallBack,
    ZipFile zipFile
  ) {
    if (validationReport == null) {
      throw new AntuException("Missing validation report");
    }
    if (validationProfile == null) {
      throw new AntuException("Missing validation profile");
    }
    NetexValidatorsRunner netexValidatorsRunner = getNetexValidatorsRunner(
      validationProfile
    );

    return null;
  }

  private NetexValidatorsRunner getNetexValidatorsRunner(
    String validationProfile
  ) {
    ValidationProfile profile = ValidationProfile
      .findById(validationProfile)
      .orElseThrow(() ->
        new AntuException("Unknown validation profile: " + validationProfile)
      );
    NetexValidatorsRunner netexValidatorsRunner = netexValidatorsRunners.get(
      profile
    );
    if (netexValidatorsRunner == null) {
      throw new AntuException(
        "Unknown validation profile " + validationProfile
      );
    }
    return netexValidatorsRunner;
  }
}
