package no.entur.antu.model;

import java.util.Objects;
import java.util.Optional;
import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.Quay;

/**
 * The id of a NeTEx quay.
 */
public record QuayId(String id) {
  public QuayId {
    Objects.requireNonNull(id, "Quay id should not be null");
    if (!isValid(id)) {
      throw new AntuException("Invalid quay id: " + id);
    }
  }

  public static QuayId ofValidId(Quay quay) {
    return Optional
      .of(quay)
      .map(Quay::getId)
      .map(QuayId::ofValidId)
      .orElse(null);
  }

  public static QuayId ofValidId(String id) {
    return id != null && isValid(id) ? new QuayId(id) : null;
  }

  public static QuayId ofNullable(String id) {
    return id == null ? null : new QuayId(id);
  }

  public static boolean isValid(String quayId) {
    return quayId.contains(":Quay:");
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return id();
  }
}
