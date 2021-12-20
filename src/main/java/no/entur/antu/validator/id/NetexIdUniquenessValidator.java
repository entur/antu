package no.entur.antu.validator.id;

import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Verify that NeTEx ids in the current file are not already present in one of the previous files.
 */
public class NetexIdUniquenessValidator {

    /**
     * Set of NeTEx elements for which id-uniqueness across lines is not verified.
     * These IDs need not be stored.
     */
    private static final HashSet<String> IGNORABLE_ELEMENTS = new HashSet<>(Arrays.asList("ResourceFrame", "SiteFrame", "CompositeFrame", "TimetableFrame", "ServiceFrame", "ServiceCalendarFrame", "VehicleScheduleFrame", "Block", "RoutePoint", "PointProjection", "ScheduledStopPoint", "PassengerStopAssignment", "NoticeAssignment"));

    private static final String MESSAGE_FORMAT_DUPLICATE_ID_ACROSS_FILES = "Duplicate element identifiers across files";

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexIdUniquenessValidator.class);


    private final RedisNetexIdRepository netexIdRepository;

    public NetexIdUniquenessValidator(RedisNetexIdRepository netexIdRepository) {
        this.netexIdRepository = netexIdRepository;
    }

    public List<ValidationReportEntry> validate(String reportId, String fileName, Set<IdVersion> netexFileLocalIds) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        final Map<String, IdVersion> netexIds;
        if (netexFileLocalIds == null) {
            // no ids were stored if the XMLSchema validation failed
            LOGGER.debug("No ids added for file {}", fileName);
            netexIds = Collections.emptyMap();
        } else {
            netexIds = netexFileLocalIds.stream().filter(idVersion -> !IGNORABLE_ELEMENTS.contains(idVersion.getElementName())).collect(Collectors.toMap(IdVersion::getId, Function.identity()));
        }
        Set<String> duplicateIds = netexIdRepository.getDuplicates(reportId, fileName, netexIds.keySet());
        if (!duplicateIds.isEmpty()) {
            for (String id : duplicateIds) {
                String validationReportEntryMessage = getIdVersionLocation(netexIds.get(id)) + MESSAGE_FORMAT_DUPLICATE_ID_ACROSS_FILES;
                validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, "NeTEx ID Consistency", ValidationReportEntrySeverity.ERROR, fileName));
            }
        }
        return validationReportEntries;
    }

    private String getIdVersionLocation(IdVersion id) {
        return "[Line " + id.getLineNumber() + ", Column " + id.getColumnNumber() + ", Id " + id.getId() + "] ";
    }
}
