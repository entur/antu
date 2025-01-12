package no.entur.antu.netexdata.collectors.activedatecollector;

import static no.entur.antu.config.cache.CacheConfig.ACTIVE_DATES_CACHE;
import static no.entur.antu.netexdata.collectors.activedatecollector.calender.CalendarUtilities.getValidityForFrameOrDefault;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.entur.antu.netexdata.collectors.activedatecollector.calender.ActiveDatesBuilder;
import no.entur.antu.netexdata.collectors.activedatecollector.calender.ServiceCalendarFrameObject;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.ServiceCalendarFrame;
import org.rutebanken.netex.model.ValidBetween;

public class ActiveDatesCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, Map<String, String>> activeDatesCache;

  public ActiveDatesCollector(
    RedissonClient redissonClient,
    Map<String, Map<String, String>> activeDatesCache
  ) {
    this.redissonClient = redissonClient;
    this.activeDatesCache = activeDatesCache;
  }

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext validationContext
  ) {
    collectData(validationContext);
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    collectData(validationContext);
  }

  private void collectData(JAXBValidationContext validationContext) {
    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects =
      parseServiceCalendarFrame(validationContext);

    Map<String, String> activeDates = getActiveDatesPerIdAsMapOfStrings(
      serviceCalendarFrameObjects
    );

    if (!activeDates.isEmpty()) {
      storeActiveDates(
        validationContext.getValidationReportId(),
        validationContext.getFileName(),
        activeDates
      );
    }
  }

  static List<ServiceCalendarFrameObject> parseServiceCalendarFrame(
    JAXBValidationContext validationContext
  ) {
    // TODO: Is it possible that the file has ServiceCalendarFrames has ServiceCalendarFrames outside the compositeFrame,
    //  while compositeFrame also exists? if yes, parse both and combine the result.
    if (validationContext.hasCompositeFrames()) {
      return validationContext
        .compositeFrames()
        .stream()
        .map(ActiveDatesCollector::getServiceCalendarFrameObjects)
        .flatMap(List::stream)
        .toList();
    } else if (validationContext.hasServiceCalendarFrames()) {
      return validationContext
        .serviceCalendarFrames()
        .stream()
        .map(ActiveDatesCollector::getServiceCalendarFrameObjects)
        .toList();
    }
    return List.of();
  }

  static Map<String, String> getActiveDatesPerIdAsMapOfStrings(
    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects
  ) {
    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<String, String> activeDatesPerDayTypes = getActiveDatesPerDayTypeId(
      serviceCalendarFrameObjects,
      activeDatesBuilder
    );

    // This is needed for DatedServiceJourneys
    Map<String, String> activeDatesPerOperationDays =
      getActiveDatesPerOperatingDayId(
        serviceCalendarFrameObjects,
        activeDatesBuilder
      );

    return Stream
      .concat(
        activeDatesPerDayTypes.entrySet().stream(),
        activeDatesPerOperationDays.entrySet().stream()
      )
      .collect(
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1)
      );
  }

  private static Map<String, String> getActiveDatesPerOperatingDayId(
    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects,
    ActiveDatesBuilder activeDatesBuilder
  ) {
    return serviceCalendarFrameObjects
      .stream()
      .map(activeDatesBuilder::buildPerOperatingDay)
      .flatMap(map -> map.entrySet().stream())
      .filter(entry -> entry.getValue().isValid())
      .collect(
        Collectors.toMap(
          entry -> entry.getKey().toString(),
          entry -> entry.getValue().toString(),
          (v1, v2) -> v1
        )
      );
  }

  private static Map<String, String> getActiveDatesPerDayTypeId(
    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects,
    ActiveDatesBuilder activeDatesBuilder
  ) {
    return serviceCalendarFrameObjects
      .stream()
      .map(activeDatesBuilder::buildPerDayType)
      .flatMap(map -> map.entrySet().stream())
      .filter(entry -> entry.getValue().isValid())
      .collect(
        Collectors.toMap(
          entry -> entry.getKey().toString(),
          entry -> entry.getValue().toString(),
          (v1, v2) -> v1
        )
      );
  }

  private void storeActiveDates(
    String validationReportId,
    String filename,
    Map<String, String> activeDates
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      String keyName =
        validationReportId + "_" + ACTIVE_DATES_CACHE + "_" + filename;

      RMap<String, String> activeDatesForFile = redissonClient.getMap(keyName);
      activeDatesForFile.putAll(activeDates);
      activeDatesCache.put(keyName, activeDatesForFile);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  private static List<ServiceCalendarFrameObject> getServiceCalendarFrameObjects(
    CompositeFrame compositeFrame
  ) {
    // When grouping Frames into a CompositeFrame, ValidityCondition must be the same for all its frames.
    // That is, ValidityCondition is not set per frame, but is implicitly controlled from the CompositeFrame.
    ValidBetween validityForCompositeFrame = getValidityForFrameOrDefault(
      compositeFrame,
      null
    );
    return compositeFrame
      .getFrames()
      .getCommonFrame()
      .stream()
      .map(JAXBElement::getValue)
      .filter(ServiceCalendarFrame.class::isInstance)
      .map(ServiceCalendarFrame.class::cast)
      .map(serviceCalendarFrame ->
        ServiceCalendarFrameObject.ofNullable(
          serviceCalendarFrame,
          validityForCompositeFrame
        )
      )
      .toList();
  }

  private static ServiceCalendarFrameObject getServiceCalendarFrameObjects(
    ServiceCalendarFrame serviceCalendarFrame
  ) {
    ValidBetween validityForServiceCalendarFrame = getValidityForFrameOrDefault(
      serviceCalendarFrame,
      null
    );
    return ServiceCalendarFrameObject.ofNullable(
      serviceCalendarFrame,
      validityForServiceCalendarFrame
    );
  }
}
