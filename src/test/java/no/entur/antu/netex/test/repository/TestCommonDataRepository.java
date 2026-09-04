package no.entur.antu.netex.test.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import org.entur.netex.validation.validator.jaxb.CommonDataRepository;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;

/**
 * A CommonDataRepository backed by plain maps, mutable after construction.
 *
 * <p>hasSharedScheduledStopPoints is an explicit flag rather than a function of the quay map,
 * because both behaviours are relied on: a test that registers no quay ids at all still expects
 * true, and {@link #of(int)} with a count of zero exists to get false.
 */
public class TestCommonDataRepository implements CommonDataRepository {

  private final Map<ScheduledStopPointId, QuayId> quayForScheduledStopPoint =
    new HashMap<>();
  private final Map<ServiceLinkId, FromToScheduledStopPointId> fromToForServiceLink =
    new HashMap<>();
  private final Map<String, String> flexibleStopPlaceRefByStopPointRef =
    new HashMap<>();

  private boolean hasSharedScheduledStopPoints = true;

  public TestCommonDataRepository() {
    //NOOP
  }

  /**
   * Return a repository that maps ScheduledStopPoint #i to Quay #i, for i in 1..numScheduledStopPoints.
   * A count of zero yields a repository that reports no shared scheduled stop points.
   */
  public static TestCommonDataRepository of(int numScheduledStopPoints) {
    TestCommonDataRepository repository = new TestCommonDataRepository();
    IntStream
      .rangeClosed(1, numScheduledStopPoints)
      .forEach(index ->
        repository.putQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + index),
          new QuayId("TST:Quay:" + index)
        )
      );
    if (numScheduledStopPoints == 0) {
      repository.withNoSharedScheduledStopPoints();
    }
    return repository;
  }

  public TestCommonDataRepository withNoSharedScheduledStopPoints() {
    this.hasSharedScheduledStopPoints = false;
    return this;
  }

  public TestCommonDataRepository putQuayId(
    ScheduledStopPointId scheduledStopPointId,
    QuayId quayId
  ) {
    quayForScheduledStopPoint.put(scheduledStopPointId, quayId);
    return this;
  }

  public TestCommonDataRepository putFromToScheduledStopPointId(
    ServiceLinkId serviceLinkId,
    FromToScheduledStopPointId fromToScheduledStopPointId
  ) {
    fromToForServiceLink.put(serviceLinkId, fromToScheduledStopPointId);
    return this;
  }

  /**
   * Register a flexible stop place for a scheduled stop point, as a shared file would.
   */
  public TestCommonDataRepository putFlexibleStopPlaceRef(
    String scheduledStopPointRef,
    String flexibleStopPlaceRef
  ) {
    flexibleStopPlaceRefByStopPointRef.put(
      scheduledStopPointRef,
      flexibleStopPlaceRef
    );
    return this;
  }

  @Override
  public boolean hasSharedScheduledStopPoints(String validationReportId) {
    return hasSharedScheduledStopPoints;
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
    return fromToForServiceLink.get(serviceLinkId);
  }

  @Override
  public String getFlexibleStopPlaceRefByStopPointRef(
    String validationReportId,
    String stopPointRef
  ) {
    return flexibleStopPlaceRefByStopPointRef.get(stopPointRef);
  }
}
