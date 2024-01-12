package no.entur.antu.model;

import no.entur.antu.exception.AntuException;

import java.util.Objects;

public record StopPlaceId(String id) {
    public StopPlaceId {
        Objects.requireNonNull(id, "Stop place id should not be null");
        if (!isValid(id)) {
            throw new AntuException("In valid stop place id: " + id);
        }
    }

    public static boolean isValid(String stopPlaceId) {
        return stopPlaceId.contains(":StopPlace:");
    }
}
