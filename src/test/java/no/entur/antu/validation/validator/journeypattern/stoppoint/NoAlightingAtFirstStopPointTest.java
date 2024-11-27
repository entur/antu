package no.entur.antu.validation.validator.journeypattern.stoppoint;

import java.util.Collections;
import java.util.List;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.netextestdata.NetexXmlTestFragment;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class NoAlightingAtFirstStopPointTest {

  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser();

  @Test
  void testForAlightingFalseAtFirstStopShouldBeOk() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          netexXmlTestFragment.journeyPattern(
            netexXmlTestFragment.pointsInSequence(
              5,
              stopPoints -> {
                // Set alighting false for first stop
                stopPoints.get(0).withForAlighting(false);
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

    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForAlightingFalseAtFirstStopWithMultipleJourneyPatternsShouldBeOk() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          5,
          jpIndex ->
            netexXmlTestFragment.pointsInSequence(
              5,
              stopPoints -> {
                // Set alighting false for first stop
                stopPoints.get(0).withForAlighting(false);
                return stopPoints;
              }
            )
        )
      )
      .create();

    List<ValidationIssue> xPathValidationReportEntries = runTestWith(
      journeyPatterns
    );

    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForAlightingFalseAtFirstStopWhenStopPlacesAreShuffledShouldBeOk() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          netexXmlTestFragment.journeyPattern(
            netexXmlTestFragment.pointsInSequence(
              5,
              list -> {
                // Set alighting false for first stop
                list.get(0).withForAlighting(false);
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

    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForAlightingTrueAtFirstStopShouldBeFailed() {
    NetexXmlTestFragment netexXmlTestFragment = new NetexXmlTestFragment();

    String journeyPatterns = netexXmlTestFragment
      .serviceFrame(
        netexXmlTestFragment.journeyPatterns(
          netexXmlTestFragment.journeyPattern(
            netexXmlTestFragment.pointsInSequence(
              5,
              stopPoints -> {
                // Set alighting true for first stop
                stopPoints.get(0).withForAlighting(true);
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

    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testForAlightingNotSetAtFirstStopWithMultipleJourneyPatternsShouldBeFailed() {
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
  void testForAlightingNotSetAtFirstStopShouldBeFailed() {
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

    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  private List<ValidationIssue> runTestWith(String journeyPatterns) {
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(journeyPatterns);

    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        NetexXmlTestFragment.TEST_CODESPACE,
        null
      );

    return new NoAlightingAtFirstStopPoint("ServiceFrame")
      .validate(xpathValidationContext);
  }
}
