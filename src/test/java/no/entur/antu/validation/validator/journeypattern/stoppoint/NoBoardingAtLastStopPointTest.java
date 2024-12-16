package no.entur.antu.validation.validator.journeypattern.stoppoint;

import java.util.Collections;
import java.util.List;
import no.entur.antu.netextestdata.NetexXmlTestFragment;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoBoardingAtLastStopPointTest {

  @Test
  void testForBoardingFalseAtLastStopShouldBeOk() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          netexXmlTestFragment.journeyPattern(
            netexXmlTestFragment.pointsInSequence(
              5,
              stopPoints -> {
                // Set boarding false for last stop
                stopPoints.get(stopPoints.size() - 1).withForBoarding(false);
                return stopPoints;
              }
            )
          )
        )
      )
      .create();

    List<ValidationIssue> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForBoardingFalseAtLastStopWithMultipleJourneyPatternsShouldBeOk() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          5,
          jpIndex ->
            netexXmlTestFragment.pointsInSequence(
              5,
              stopPoints -> {
                // Set boarding false for last stop
                stopPoints.get(stopPoints.size() - 1).withForBoarding(false);
                return stopPoints;
              }
            )
        )
      )
      .create();

    List<ValidationIssue> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForBoardingFalseAtLastStopWhenStopPlacesAreShuffledShouldBeOk() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          netexXmlTestFragment.journeyPattern(
            netexXmlTestFragment.pointsInSequence(
              5,
              list -> {
                // Set boarding false for last stop
                list.get(list.size() - 1).withForBoarding(false);
                // Shuffling the list, for testing the sorting.
                Collections.shuffle(list);
                return list;
              }
            )
          )
        )
      )
      .create();

    List<ValidationIssue> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForBoardingTrueAtLastStopShouldBeFailed() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          netexXmlTestFragment.journeyPattern(
            netexXmlTestFragment.pointsInSequence(
              5,
              stopPoints -> {
                // Set boarding true for last stop
                stopPoints.get(stopPoints.size() - 1).withForBoarding(true);
                return stopPoints;
              }
            )
          )
        )
      )
      .create();

    List<ValidationIssue> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForBoardingNotSetAtLastStopWithMultipleJourneyPatternsShouldBeFailed() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          5,
          jpIndex -> netexXmlTestFragment.pointsInSequence(5)
        )
      )
      .create();

    List<ValidationIssue> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
    Assertions.assertEquals(5, xPathValidationReportEntries.size());
  }

  @Test
  void testForBoardingNotSetAtLastStopShouldBeFailed() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          netexXmlTestFragment.journeyPattern(
            netexXmlTestFragment.pointsInSequence(5)
          )
        )
      )
      .create();

    List<ValidationIssue> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  private List<ValidationIssue> runTestWith(String journeyPatterns) {
    XPathRuleValidationContext xpathValidationContext =
      TestValidationContextBuilder
        .ofNetexFragment(journeyPatterns)
        .withCodespace(NetexXmlTestFragment.TEST_CODESPACE)
        .build();

    return new NoBoardingAtLastStopPoint().validate(xpathValidationContext);
  }
}
