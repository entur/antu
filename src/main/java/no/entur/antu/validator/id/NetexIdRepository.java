package no.entur.antu.validator.id;

import java.util.Set;

/**
 * Repository for NeTEx IDs to check uniqueness across lines.
 */
public interface NetexIdRepository {

    /**
     * Return  the set of NeTEx Ids that are present both in the current file and in the set of NeTEX ids already found in other common files or line files.
     *
     * @param reportId
     * @param filename
     * @param localIds
     * @return
     */
    Set<String> getDuplicates(String reportId, String filename, Set<String> localIds);
}
