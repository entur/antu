package no.entur.antu.model;

import java.util.Objects;
import java.util.Optional;
import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.MultilingualString;

public record LineInfo(String lineId, String lineName, String fileName) {
  public LineInfo {
    Objects.requireNonNull(lineId, "Line id should not be null");
    Objects.requireNonNull(lineName, "Line name should not be null");
    Objects.requireNonNull(fileName, "File name should not be null");
  }

  public static LineInfo of(Line line, String fileName) {
    return new LineInfo(
      line.getId(),
      Optional
        .ofNullable(line.getName())
        .map(MultilingualString::getValue)
        .orElse(null),
      fileName
    );
  }

  public static LineInfo of(FlexibleLine flexibleLine, String fileName) {
    return new LineInfo(
      flexibleLine.getId(),
      Optional
        .ofNullable(flexibleLine.getName())
        .map(MultilingualString::getValue)
        .orElse(null),
      fileName
    );
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return lineId + "§" + lineName + "§" + fileName;
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  public static LineInfo fromString(String lineInfo) {
    if (lineInfo != null) {
      String[] split = lineInfo.split("§");
      if (split.length == 3) {
        return new LineInfo(split[0], split[1], split[2]);
      } else {
        throw new AntuException("Invalid lineInfo string: " + lineInfo);
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof LineInfo lineInfo)) return false;
    return Objects.equals(lineName, lineInfo.lineName);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(lineName);
  }
}
