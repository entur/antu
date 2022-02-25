package no.entur.antu.sweden.stop;

import java.util.Set;

/**
 * Repository that stores StopPlace and Quay NeTEX ID found in SiteFrame in the dataset.
 * (NSR references are not used for swedish timetable data).
 */
public interface SwedenStopPlaceNetexIdRepository {

    /**
     * Return the StopPlace and Quay NeTEx ids found in the dataset.
     * @param reportId the current report id.
     * @return the StopPlace and Quay NeTEx ids found in the dataset.
     */
    Set<String> getSharedStopPlaceAndQuayIds(String reportId);

    /**
     * Add the StopPlace and Quay NeTEx ids found in the dataset to thew repository.
     * @param reportId the current report id.
     * @param stopPlaceAndQuayIds the IDs to be added in the repository.
     */
    void addSharedStopPlaceAndQuayIds(String reportId, Set<String> stopPlaceAndQuayIds);

    /**
     * Delete cached Ids for a given report.
     * @param reportId the current report id.
     */
    void cleanUp(String reportId);
}
