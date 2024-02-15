package no.entur.antu.validator.stoppoint;

import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * Validate that the first StopPointInJourneyPattern does not allow alighting.
 */
public class NoAlightingAtFirstStopPointInJourneyPattern
  extends ValidateNotExist {

  public NoAlightingAtFirstStopPointInJourneyPattern(String path) {
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
          )[1][count(ForAlighting) = 0 or ForAlighting != 'false']
        }
      )
      """.formatted(
          path
        ),
      "First StopPointInJourneyPattern must not allow alighting",
      "JOURNEY_PATTERN_NO_ALIGHTING_ALLOWED_AT_FIRST_STOP"
    );
  }
}
