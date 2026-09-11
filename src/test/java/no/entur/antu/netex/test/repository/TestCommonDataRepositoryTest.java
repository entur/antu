package no.entur.antu.netex.test.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;
import org.junit.jupiter.api.Test;

class TestCommonDataRepositoryTest {

  private static final String REPORT_ID = "TestReport";

  @Test
  void emptyRepositoryReportsSharedScheduledStopPointsByDefault() {
    assertTrue(
      new TestCommonDataRepository().hasSharedScheduledStopPoints(REPORT_ID)
    );
  }

  @Test
  void withNoSharedScheduledStopPointsOverridesTheDefault() {
    TestCommonDataRepository repository = new TestCommonDataRepository();
    repository.withNoSharedScheduledStopPoints();
    assertFalse(repository.hasSharedScheduledStopPoints(REPORT_ID));
  }

  @Test
  void ofZeroReportsNoSharedScheduledStopPoints() {
    assertFalse(
      TestCommonDataRepository.of(0).hasSharedScheduledStopPoints(REPORT_ID)
    );
  }

  @Test
  void ofNMapsScheduledStopPointIToQuayI() {
    TestCommonDataRepository repository = TestCommonDataRepository.of(2);

    assertTrue(repository.hasSharedScheduledStopPoints(REPORT_ID));
    assertEquals(
      new QuayId("TST:Quay:2"),
      repository.quayIdForScheduledStopPoint(
        new ScheduledStopPointId("TST:ScheduledStopPoint:2"),
        REPORT_ID
      )
    );
    assertNull(
      repository.quayIdForScheduledStopPoint(
        new ScheduledStopPointId("TST:ScheduledStopPoint:3"),
        REPORT_ID
      )
    );
  }

  @Test
  void putQuayIdIsVisibleImmediately() {
    TestCommonDataRepository repository = new TestCommonDataRepository();
    ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:7"
    );

    repository.putQuayId(scheduledStopPointId, new QuayId("TST:Quay:7"));

    assertEquals(
      new QuayId("TST:Quay:7"),
      repository.quayIdForScheduledStopPoint(scheduledStopPointId, REPORT_ID)
    );
  }

  @Test
  void quayIdForNullScheduledStopPointIsNull() {
    assertNull(
      new TestCommonDataRepository()
        .quayIdForScheduledStopPoint(null, REPORT_ID)
    );
  }

  @Test
  void putFromToScheduledStopPointIdIsVisibleImmediately() {
    TestCommonDataRepository repository = new TestCommonDataRepository();
    ServiceLinkId serviceLinkId = new ServiceLinkId("TST:ServiceLink:1");
    FromToScheduledStopPointId fromTo = new FromToScheduledStopPointId(
      new ScheduledStopPointId("TST:ScheduledStopPoint:1"),
      new ScheduledStopPointId("TST:ScheduledStopPoint:2")
    );

    repository.putFromToScheduledStopPointId(serviceLinkId, fromTo);

    assertEquals(
      fromTo,
      repository.fromToScheduledStopPointIdForServiceLink(
        serviceLinkId,
        REPORT_ID
      )
    );
  }

  @Test
  void fromToScheduledStopPointIdForAnUnknownServiceLinkIsNull() {
    assertNull(
      new TestCommonDataRepository()
        .fromToScheduledStopPointIdForServiceLink(
          new ServiceLinkId("TST:ServiceLink:9"),
          REPORT_ID
        )
    );
  }

  @Test
  void putFlexibleStopPlaceRefIsVisibleImmediately() {
    TestCommonDataRepository repository = new TestCommonDataRepository();

    repository.putFlexibleStopPlaceRef(
      "TST:ScheduledStopPoint:1",
      "TST:FlexibleStopPlace:1"
    );

    assertEquals(
      "TST:FlexibleStopPlace:1",
      repository.getFlexibleStopPlaceRefByStopPointRef(
        REPORT_ID,
        "TST:ScheduledStopPoint:1"
      )
    );
  }

  @Test
  void anUnknownStopPointHasNoFlexibleStopPlaceRef() {
    assertNull(
      new TestCommonDataRepository()
        .getFlexibleStopPlaceRefByStopPointRef(
          REPORT_ID,
          "TST:ScheduledStopPoint:9"
        )
    );
  }
}
