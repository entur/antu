package no.entur.antu.common.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.entur.netex.validation.validator.jaxb.CommonDataRepository;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;

public class TestCommonDataRepository implements CommonDataRepository {

  private final String VALIDATION_REPORT_KEY = "VALIDATION_REPORT_KEY";

  private final Map<ScheduledStopPointId, QuayId> quayForScheduledStopPoint;
  private final HashMap<String, HashMap<String, String>> flexibleStopPlaceRefByStopPointRef;

  public TestCommonDataRepository(
    Map<ScheduledStopPointId, QuayId> quayForScheduledStopPoint,
    HashMap<String, String> stopPointRefToFlexibleStopPlaceRefMap
  ) {
    this.quayForScheduledStopPoint = quayForScheduledStopPoint;
    this.flexibleStopPlaceRefByStopPointRef = new HashMap<>();
    flexibleStopPlaceRefByStopPointRef.put(
      VALIDATION_REPORT_KEY,
      stopPointRefToFlexibleStopPlaceRefMap
    );
  }

  /**
   * Return a common data repository that maps ScheduledStopPoint #i to Quay #i
   */
  public static CommonDataRepository of(int numScheduledStopPoints) {
    Map<ScheduledStopPointId, QuayId> stopPointIdQuayIdMap = IntStream
      .rangeClosed(1, numScheduledStopPoints)
      .boxed()
      .collect(
        Collectors.toUnmodifiableMap(
          index -> new ScheduledStopPointId("TST:ScheduledStopPoint:" + index),
          index -> new QuayId("TST:Quay:" + index)
        )
      );

    return new TestCommonDataRepository(stopPointIdQuayIdMap, new HashMap<>());
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

  @Override
  public String getFlexibleStopPlaceRefByStopPointRef(
    String validationReportId,
    String stopPointRef
  ) {
    return this.flexibleStopPlaceRefByStopPointRef.get(VALIDATION_REPORT_KEY)
      .get(stopPointRef);
  }

  @Override
  public Set<String> authorityRefs(String validationReportId) {
    return Set.of();
  }

  public HashMap<String, String> getFlexibleStopPlaceRefByStopPointRef() {
    return this.flexibleStopPlaceRefByStopPointRef.get(VALIDATION_REPORT_KEY);
  }
}
