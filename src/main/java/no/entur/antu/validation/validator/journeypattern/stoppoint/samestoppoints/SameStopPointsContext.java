package no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints;

import java.util.List;
import java.util.Objects;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;

record SameStopPointsContext(String journeyPatternId, List<String> pointRefs) {
  public static SameStopPointsContext of(JourneyPattern journeyPattern) {
    return new SameStopPointsContext(
      journeyPattern.getId(),
      AntuNetexData
        .stopPointsInJourneyPattern(journeyPattern)
        .map(PointInLinkSequence_VersionedChildStructure::getId)
        .filter(Objects::nonNull)
        .toList()
    );
  }

  /**
   * The equals method is used by the groupingBy collector in the validate method.
   *
   * @param obj the reference object with which to compare.
   * @return true if the two lists of pointRefs are equal, false otherwise.
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SameStopPointsContext that) {
      return this.pointRefs().equals(that.pointRefs());
    }
    return false;
  }

  /**
   * The hashCode method is used by the groupingBy collector in the validate method.
   *
   * @return a hash code value for this object.
   */
  @Override
  public int hashCode() {
    return pointRefs.hashCode();
  }
}
