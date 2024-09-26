package no.entur.antu.validation.validator.journeypattern.stoppoint;

import java.util.Collections;
import java.util.List;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.netextestdata.NetexXmlTestFragment;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoBoardingAtLastStopPointTest {

  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser();

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

    List<XPathValidationReportEntry> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertNotNull(xPathValidationReportEntries);
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

    List<XPathValidationReportEntry> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertNotNull(xPathValidationReportEntries);
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

    List<XPathValidationReportEntry> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertNotNull(xPathValidationReportEntries);
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

    List<XPathValidationReportEntry> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertNotNull(xPathValidationReportEntries);
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

    List<XPathValidationReportEntry> xPathValidationReportEntries = runTestWith(
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

    List<XPathValidationReportEntry> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  private List<XPathValidationReportEntry> runTestWith(String journeyPatterns) {
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(journeyPatterns);

    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        NetexXmlTestFragment.TEST_CODESPACE,
        null
      );

    return new NoBoardingAtLastStopPoint("ServiceFrame")
      .validate(xpathValidationContext);
  }
}
