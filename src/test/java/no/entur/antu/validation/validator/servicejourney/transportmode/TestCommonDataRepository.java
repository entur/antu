package no.entur.antu.validation.validator.servicejourney.transportmode;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.entur.netex.validation.validator.jaxb.CommonDataRepository;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;

class TestCommonDataRepository implements CommonDataRepository {

  private final Map<ScheduledStopPointId, QuayId> quayForScheduledStopPoint;

  TestCommonDataRepository(
    Map<ScheduledStopPointId, QuayId> quayForScheduledStopPoint
  ) {
    this.quayForScheduledStopPoint = quayForScheduledStopPoint;
  }

  /**
   * Return a common data repository that maps ScheduledStopPoint #i to Quay #i
   */
  static CommonDataRepository of(int numScheduledStopPoints) {
    Map<ScheduledStopPointId, QuayId> stopPointIdQuayIdMap = IntStream
      .rangeClosed(1, numScheduledStopPoints)
      .boxed()
      .collect(
        Collectors.toUnmodifiableMap(
          index -> new ScheduledStopPointId("TST:ScheduledStopPoint:" + index),
          index -> new QuayId("TST:Quay:" + index)
        )
      );

    return new TestCommonDataRepository(stopPointIdQuayIdMap);
  }

  @Override
  public boolean hasSharedScheduledStopPoints(String validationReportId) {
    return !quayForScheduledStopPoint.isEmpty();
  }

  @Override
  public QuayId quayIdForScheduledStopPoint(
    ScheduledStopPointId scheduledStopPointId,
    String validationReportId
  ) {
    if (scheduledStopPointId == null) {
      return null;
    }
    return quayForScheduledStopPoint.get(scheduledStopPointId);
  }

  @Override
  public FromToScheduledStopPointId fromToScheduledStopPointIdForServiceLink(
    ServiceLinkId serviceLinkId,
    String validationReportId
  ) {
    return null;
  }
}
