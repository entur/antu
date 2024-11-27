package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import com.google.common.collect.Multimap;
import java.util.Map;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;

public record CalendarData(
  Map<DayTypeId, DayType> dayTypes,
  Map<String, OperatingPeriod> operatingPeriods,
  Map<OperatingDayId, OperatingDay> operatingDays,
  Multimap<DayTypeId, DayTypeAssignment> dayTypeAssignments
) {}
