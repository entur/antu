package no.entur.antu.validator.journeypattern.stoppoint;

import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * Validate that the last StopPointInJourneyPattern does not allow boarding.
 */
public class NoBoardingAtLastStopPoint
  extends ValidateNotExist {

  public NoBoardingAtLastStopPoint(String path) {
    super(
      """
      for-each(
        %s/journeyPatterns/JourneyPattern,
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
      """.formatted(
          path
        ),
      "Last StopPointInJourneyPattern must not allow boarding",
      "JOURNEY_PATTERN_NO_BOARDING_ALLOWED_AT_LAST_STOP"
    );
  }
}
