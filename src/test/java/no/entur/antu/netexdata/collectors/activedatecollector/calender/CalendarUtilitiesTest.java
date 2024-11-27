package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.Multimap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.ValidBetween;
import org.rutebanken.netex.model.ValidityConditions_RelStructure;

class CalendarUtilitiesTest {

  @Test
  void testToMultimap() {
    // Arrange
    NetexEntitiesTestFactory.CreateDayType dayType1 =
      new NetexEntitiesTestFactory.CreateDayType(1);
    DayTypeAssignment assignment1 =
      new NetexEntitiesTestFactory.CreateDayTypeAssignment(1, dayType1)
        .create();
    NetexEntitiesTestFactory.CreateDayType dayType2 =
      new NetexEntitiesTestFactory.CreateDayType(2);
    DayTypeAssignment assignment2 =
      new NetexEntitiesTestFactory.CreateDayTypeAssignment(2, dayType2)
        .create();
    NetexEntitiesTestFactory.CreateDayType dayType3 =
      new NetexEntitiesTestFactory.CreateDayType(3);
    DayTypeAssignment assignment3 =
      new NetexEntitiesTestFactory.CreateDayTypeAssignment(1, dayType3)
        .create();

    List<DayTypeAssignment> assignments = List.of(
      assignment1,
      assignment2,
      assignment3
    );

    Function<DayTypeAssignment, DayTypeId> keyMapper = DayTypeId::of;
    Function<DayTypeAssignment, DayTypeAssignment> valueMapper =
      Function.identity();

    // Act
    Multimap<DayTypeId, DayTypeAssignment> result = assignments
      .stream()
      .collect(CalendarUtilities.toMultimap(keyMapper, valueMapper));

    // Assert
    assertTrue(result.get(new DayTypeId(dayType1.ref())).contains(assignment1));
    assertTrue(result.get(new DayTypeId(dayType2.ref())).contains(assignment2));
    assertTrue(result.get(new DayTypeId(dayType3.ref())).contains(assignment3));
  }

  @Test
  void testGetOrDefault() {
    assertEquals("default", CalendarUtilities.getOrDefault(null, "default"));
    assertEquals("value", CalendarUtilities.getOrDefault("value", "default"));
  }

  @Test
  void testIsWithinValidRange() {
    ValidBetween validBetween = new ValidBetween()
      .withFromDate(LocalDateTime.of(2022, 1, 1, 0, 0))
      .withToDate(LocalDateTime.of(2022, 12, 31, 23, 59));

    LocalDateTime withinRange = LocalDateTime.of(2022, 6, 15, 12, 0);
    LocalDateTime beforeRange = LocalDateTime.of(2021, 12, 31, 23, 59);
    LocalDateTime afterRange = LocalDateTime.of(2023, 1, 1, 0, 0);

    assertTrue(CalendarUtilities.isWithinValidRange(withinRange, validBetween));
    assertFalse(
      CalendarUtilities.isWithinValidRange(beforeRange, validBetween)
    );
    assertFalse(CalendarUtilities.isWithinValidRange(afterRange, validBetween));
  }

  @Test
  void testGetValidityForFrameOrDefault() {
    Common_VersionFrameStructure frameStructure = mock(
      Common_VersionFrameStructure.class
    );

    ValidBetween defaultValidity = new ValidBetween()
      .withFromDate(LocalDateTime.of(2022, 1, 1, 0, 0))
      .withToDate(LocalDateTime.of(2022, 12, 31, 23, 59));

    when(frameStructure.getValidBetween()).thenReturn(null);
    when(frameStructure.getContentValidityConditions()).thenReturn(null);
    when(frameStructure.getValidityConditions()).thenReturn(null);

    ValidBetween result = CalendarUtilities.getValidityForFrameOrDefault(
      frameStructure,
      defaultValidity
    );

    assertEquals(defaultValidity, result);
  }

  @Test
  void testGetValidBetween_withEmptyStructure() {
    ValidityConditions_RelStructure structure = mock(
      ValidityConditions_RelStructure.class
    );
    when(structure.getValidityConditionRefOrValidBetweenOrValidityCondition_())
      .thenReturn(List.of());

    ValidBetween result = CalendarUtilities.getValidBetween(structure);

    assertNull(result);
  }

  @Test
  void testGetValidBetween_withValidCondition() {
    ValidityConditions_RelStructure structure = mock(
      ValidityConditions_RelStructure.class
    );
    ValidBetween validBetween = new ValidBetween()
      .withFromDate(LocalDateTime.of(2023, 1, 1, 0, 0))
      .withToDate(LocalDateTime.of(2023, 12, 31, 23, 59));

    when(structure.getValidityConditionRefOrValidBetweenOrValidityCondition_())
      .thenReturn(List.of(validBetween));

    ValidBetween result = CalendarUtilities.getValidBetween(structure);

    assertNotNull(result);
    assertEquals(LocalDateTime.of(2023, 1, 1, 0, 0), result.getFromDate());
    assertEquals(LocalDateTime.of(2023, 12, 31, 23, 59), result.getToDate());
  }
}
