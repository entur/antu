package no.entur.antu.validator.id;

import java.util.Set;

public interface CommonNetexIdRepository {
    Set<String> getCommonNetexIds(String reportId);

    void addCommonNetexIds(String reportId, Set<IdVersion> commonIds);

    void cleanUp(String reportId);
}
