package no.entur.antu.validation.validator.journeypattern.stoppoint;

import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * Validate that the last StopPointInJourneyPattern does not allow boarding.
 * Chouette reference: 3-JourneyPattern-4
 */
public class NoBoardingAtLastStopPoint extends ValidateNotExist {

  public NoBoardingAtLastStopPoint() {
    super(
      """
      for-each(
        journeyPatterns/JourneyPattern,
        function($jp) {
          sort(
            $jp/pointsInSequence/StopPointInJourneyPattern,
            (),
            function($sp) {
              ($sp/xs:integer(number(@order)))
            }
          )[last()][count(ForBoarding) = 0 or ForBoarding != 'false']
        }
      )
      """,
      "JOURNEY_PATTERN_NO_BOARDING_ALLOWED_AT_LAST_STOP",
      "Journey Pattern - No boarding on last stop",
      "Last StopPointInJourneyPattern must not allow boarding",
      Severity.ERROR
    );
  }
}
