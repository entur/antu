package no.entur.antu.netexdata.collectors;

import jakarta.xml.bind.JAXBElement;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.rutebanken.netex.model.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class ServiceJourneyActiveDatesCollector extends NetexDataCollector {

    private final RedissonClient redissonClient;
    private final Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDates;
    private final Map<String, Map<String, List<LocalDateTime>>> serviceJourneyActiveDates;

    public ServiceJourneyActiveDatesCollector(RedissonClient redissonClient,
                                              // maps validationReportId -> map of dayTypeRef-> activeDate[]
                                              Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache,
                                              // maps validationReportId -> map of serviceJourney -> activeDate[]
                                              Map<String, Map<String, List<LocalDateTime>>> serviceJourneyActiveDatesCache) {
        this.redissonClient = redissonClient;
        this.dayTypeActiveDates = dayTypeActiveDatesCache;
        this.serviceJourneyActiveDates = serviceJourneyActiveDatesCache;
    }

    @Override
    protected void collectDataFromLineFile(JAXBValidationContext jaxbValidationContext) {
        var commonDayTypeActiveDates = this.dayTypeActiveDates.get(jaxbValidationContext.getValidationReportId());
        var lineDayTypesToActiveDates = getDayTypesToActiveDates(jaxbValidationContext);

        // Så kan vi samle opp daytypes fra servicejourneys + operatingdays fra datedservicejourneys

        Map<String, List<LocalDateTime>> serviceJourneyToDates = jaxbValidationContext.serviceJourneys().stream().map(
                serviceJourney -> {
            DayTypeRefs_RelStructure dayTypeRefsRelStructure = serviceJourney.getDayTypes();
            List<JAXBElement<? extends DayTypeRefStructure>> dayTypeRefs = dayTypeRefsRelStructure.getDayTypeRef();
            List<LocalDateTime> serviceJourneyDates = new ArrayList<>();
            dayTypeRefs.forEach(dt -> {
                var dtId = dt.getValue().getRef();
                serviceJourneyDates.addAll(commonDayTypeActiveDates.get(dtId));
                serviceJourneyDates.addAll(lineDayTypesToActiveDates.get(dtId));
            });
            return Map.entry(
                    serviceJourney.getId(),
                    serviceJourneyDates
            );
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // For hver datedservicejourney må vi også legge til datoene som er referert til av operating day ref til i mappet
        // for servicejourneytodates. For eksempel kan vi ha:
        // ServiceJourney<A>
        // OperatingDay<B, 25.12.25>
        // DatedServiceJourney<A, B>
        // ==> ServiceJourney -> [25.12.25]
        addServiceJourneyActiveDates(jaxbValidationContext.getValidationReportId(), serviceJourneyToDates);
    }

    @Override
    protected void collectDataFromCommonFile(JAXBValidationContext jaxbValidationContext) {
        Map<String, List<LocalDateTime>> dayTypesToActiveDates = getDayTypesToActiveDates(jaxbValidationContext);

        addActiveDates(jaxbValidationContext.getValidationReportId(), dayTypesToActiveDates);
    }

    private Map<String, List<LocalDateTime>> getDayTypesToActiveDates(JAXBValidationContext jaxbValidationContext) {
        var operatingDaysToCalendarDate = jaxbValidationContext.getNetexEntitiesIndex().getOperatingDayIndex().getAll().stream().map(operatingDay -> Map.entry(
                operatingDay.getId(),
                operatingDay.getCalendarDate()
        )).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return jaxbValidationContext.getNetexEntitiesIndex().getDayTypeIndex().getAll().stream().map(dayType -> {
            List<LocalDateTime> dates = new ArrayList<>();
            jaxbValidationContext.getNetexEntitiesIndex().getDayTypeAssignmentsByDayTypeIdIndex().get(dayType.getId()).forEach(dayTypeAssignment -> {
                if (dayTypeAssignment.getDate() != null) {
                    dates.add(dayTypeAssignment.getDate());
                }
                if (dayTypeAssignment.getOperatingDayRef() != null) {
                    dates.add(operatingDaysToCalendarDate.get(dayTypeAssignment.getOperatingDayRef().getRef()));
                }
                if (dayTypeAssignment.getOperatingPeriodRef() != null) {
                    List<PropertyOfDay> propertyOfDayElements = dayType.getProperties().getPropertyOfDay();
                    OperatingPeriod period = jaxbValidationContext.getNetexEntitiesIndex().getOperatingPeriodIndex().get(dayTypeAssignment.getOperatingPeriodRef().getValue().getRef());
                    Set<DayOfWeekEnumeration> daysOfWeek = new HashSet<>(propertyOfDayElements.stream().flatMap(propertyOfDay -> propertyOfDay.getDaysOfWeek().stream()).toList());
                    dates.addAll(computeDatesForPeriodAndWeekday(period, daysOfWeek));
                }
            });
            return Map.entry(
                    dayType.getId(),
                    dates
            );
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void addServiceJourneyActiveDates(String validationReportId, Map<String, List<LocalDateTime>> newServiceJourneyActiveDates) {
        RLock lock = redissonClient.getLock(validationReportId);
        try {
            lock.lock();
            var reportServiceJourneyActiveDates = this.serviceJourneyActiveDates.get(validationReportId);

            newServiceJourneyActiveDates.forEach(
                    (serviceJourneyId, activeDates) -> {
                        reportServiceJourneyActiveDates.computeIfAbsent(serviceJourneyId, k -> new ArrayList<>())
                                .addAll(activeDates);
                    }
            );
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void addActiveDates(String validationReportId, Map<String, List<LocalDateTime>> dayTypeActiveDates) {
        RLock lock = redissonClient.getLock(validationReportId);
        try {
            lock.lock();
            var reportDayTypeActiveDates = this.dayTypeActiveDates.get(validationReportId);

            dayTypeActiveDates.forEach(
                    (dayTypeRef, activeDates) -> {
                        reportDayTypeActiveDates.computeIfAbsent(dayTypeRef, k -> new ArrayList<>())
                                .addAll(activeDates);
                    }
            );
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Set<LocalDateTime> computeDatesForPeriodAndWeekday(OperatingPeriod period, Set<DayOfWeekEnumeration> daysOfWeekEnumeration) {
        Set<DayOfWeek> daysOfWeek = mapDayOfWeeks(daysOfWeekEnumeration);
        Set<LocalDateTime> dates = new HashSet<>();
        LocalDateTime fromDate = period.getFromDate();
        LocalDateTime toDate = period.getToDate();
        for (LocalDateTime d = fromDate; d.isBefore(toDate); d = d.plusDays(1)) {
            if (daysOfWeek.contains(d.getDayOfWeek())) {
                dates.add(d);
            }
        }

        return dates;
    }

    static Set<DayOfWeek> mapDayOfWeeks(Collection<DayOfWeekEnumeration> values) {
        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        for (DayOfWeekEnumeration it : values) {
            result.addAll(mapDayOfWeek(it));
        }
        return result;
    }

    static Set<DayOfWeek> mapDayOfWeek(DayOfWeekEnumeration value) {
        switch (value) {
            case MONDAY:
                return EnumSet.of(DayOfWeek.MONDAY);
            case TUESDAY:
                return EnumSet.of(DayOfWeek.TUESDAY);
            case WEDNESDAY:
                return EnumSet.of(DayOfWeek.WEDNESDAY);
            case THURSDAY:
                return EnumSet.of(DayOfWeek.THURSDAY);
            case FRIDAY:
                return EnumSet.of(DayOfWeek.FRIDAY);
            case SATURDAY:
                return EnumSet.of(DayOfWeek.SATURDAY);
            case SUNDAY:
                return EnumSet.of(DayOfWeek.SUNDAY);
            case WEEKDAYS:
                return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
            case WEEKEND:
                return EnumSet.range(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            case EVERYDAY:
                return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SUNDAY);
            case NONE:
                return EnumSet.noneOf(DayOfWeek.class);
        }
        throw new IllegalArgumentException("Day of week enum mapping missing: " + value);
    }
}
