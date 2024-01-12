package no.entur.antu.model;

import no.entur.antu.exception.AntuException;
import java.util.Objects;

public record QuayId(String id) {
    public QuayId {
        Objects.requireNonNull(id, "Quay id should not be null");
        if (!isValid(id)) {
                throw new AntuException("Invalid quay id: " + id);
        }
    }

    public static boolean isValid(String quayId) {
        return quayId.contains(":Quay:");
    }

    @Override
    public String toString() {
        return id();
    }
}
