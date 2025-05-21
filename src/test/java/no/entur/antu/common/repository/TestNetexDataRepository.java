package no.entur.antu.common.repository;

import no.entur.antu.netexdata.NetexDataRepositoryLoader;
import org.entur.netex.validation.validator.model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestNetexDataRepository implements NetexDataRepositoryLoader {
    private Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> serviceJourneyIdToActiveDatesByValidationReportId;
    private Map<String, List<ServiceJourneyInterchangeInfo>> serviceJourneyInterchangeInfos;
    private Map<String, Map<ServiceJourneyId, List<ServiceJourneyStop>>> serviceJourneyStopsMap;

    @Override
    public List<SimpleLine> lineNames(String validationReportId) {
        return List.of();
    }

    @Override
    public Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops(
            String validationReportId
    ) {
        return this.serviceJourneyStopsMap.get(validationReportId);
    }

    @Override
    public List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos(
            String validationReportId
    ) {
        return this.serviceJourneyInterchangeInfos.get(validationReportId);
    }

    public void addServiceJourneyInterchangeInfo(String validationReportId, ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo) {
        ArrayList<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList = new ArrayList<>();
        if (this.serviceJourneyInterchangeInfos == null) {
            this.serviceJourneyInterchangeInfos = new HashMap<>();
            serviceJourneyInterchangeInfoList.add(serviceJourneyInterchangeInfo);
            this.serviceJourneyInterchangeInfos.put(validationReportId, serviceJourneyInterchangeInfoList);
        } else if (this.serviceJourneyInterchangeInfos.containsKey(validationReportId)) {
            this.serviceJourneyInterchangeInfos.get(validationReportId).add(serviceJourneyInterchangeInfo);
        } else {
            serviceJourneyInterchangeInfoList.add(serviceJourneyInterchangeInfo);
            this.serviceJourneyInterchangeInfos.put(validationReportId, serviceJourneyInterchangeInfoList);
        }
    }

    @Override
    public Map<ServiceJourneyId, List<DayTypeId>> serviceJourneyDayTypes(
            String validationReportId
    ) {
        return Map.of();
    }

    @Override
    public Map<ActiveDatesId, ActiveDates> activeDates(
            String validationReportId
    ) {
        return Map.of();
    }

    @Override
    public Map<ServiceJourneyId, List<OperatingDayId>> serviceJourneyOperatingDays(
            String validationReportId
    ) {
        return Map.of();
    }

    @Override
    public void cleanUp(String validationReportId) {}

    @Override
    public Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyIdToActiveDates(String validationReportId) {
        return serviceJourneyIdToActiveDatesByValidationReportId.get(validationReportId);
    }

    public void putServiceJourneyIdToActiveDates(String validationReportId, Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyIdToActiveDates) {
        Map.Entry<String, Map<ServiceJourneyId, List<LocalDateTime>>> entry = Map.entry(validationReportId, serviceJourneyIdToActiveDates);
        if (this.serviceJourneyIdToActiveDatesByValidationReportId == null) {
            this.serviceJourneyIdToActiveDatesByValidationReportId = new HashMap<>();
            this.serviceJourneyIdToActiveDatesByValidationReportId.put(validationReportId, entry.getValue());
            return;
        }
        this.serviceJourneyIdToActiveDatesByValidationReportId.put(validationReportId, entry.getValue());
    }

    public void putServiceJourneyStop(String validationReportId, Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops) {
        Map.Entry<String, Map<ServiceJourneyId, List<ServiceJourneyStop>>> entry = Map.entry(validationReportId, serviceJourneyStops);
        if (this.serviceJourneyStopsMap == null) {
            this.serviceJourneyStopsMap = new HashMap<>();
            this.serviceJourneyStopsMap.put(validationReportId, entry.getValue());
            return;
        }
        this.serviceJourneyStopsMap.put(validationReportId, entry.getValue());
    }
}
