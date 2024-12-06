package no.entur.antu.validation.validator.journeypattern.stoppoint;

import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;

/**
 * Validate that the first StopPointInJourneyPattern does not allow alighting.
 * Chouette reference: 3-JourneyPattern-5
 */
public class NoAlightingAtFirstStopPoint extends ValidateNotExist {

  public NoAlightingAtFirstStopPoint(String path) {
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
      "JOURNEY_PATTERN_NO_ALIGHTING_ALLOWED_AT_FIRST_STOP",
      "Journey Pattern - No alighting on first stop",
      "First StopPointInJourneyPattern must not allow alighting",
      Severity.ERROR
    );
  }
}
