package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.config.ValidationParametersConfig;
import no.entur.antu.organisation.SimpleOrganisationAliasRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.validator.xpath.XPathRuleValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Test;

class DeprecatedDatedServiceJourneyRefTest {

  private static final String RULE_CODE =
    "DEPRECATED_DATED_SERVICE_JOURNEY_REF";
  private static final String CODESPACE = "TST";
  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(
    Set.of("SiteFrame")
  );

  @Test
  void testOldFormatWithDatedServiceJourneyRefShouldFail() {
    String xml = publicationDelivery(
      """
      <DatedServiceJourney version="1" id="TST:DatedServiceJourney:1">
        <ServiceJourneyRef ref="TST:ServiceJourney:1" version="1"/>
        <DatedServiceJourneyRef ref="TST:DatedServiceJourney:2"/>
        <OperatingDayRef ref="TST:OperatingDay:1"/>
      </DatedServiceJourney>
      """
    );

    List<ValidationIssue> issues = validate(xml);

    assertThat(issues.size(), is(1));
  }

  @Test
  void testOldFormatMultipleDatedServiceJourneyRefsShouldFail() {
    String xml = publicationDelivery(
      """
      <DatedServiceJourney version="1" id="TST:DatedServiceJourney:1">
        <ServiceJourneyRef ref="TST:ServiceJourney:1" version="1"/>
        <DatedServiceJourneyRef ref="TST:DatedServiceJourney:2"/>
        <OperatingDayRef ref="TST:OperatingDay:1"/>
      </DatedServiceJourney>
      <DatedServiceJourney version="1" id="TST:DatedServiceJourney:3">
        <ServiceJourneyRef ref="TST:ServiceJourney:2" version="1"/>
        <DatedServiceJourneyRef ref="TST:DatedServiceJourney:4"/>
        <OperatingDayRef ref="TST:OperatingDay:1"/>
      </DatedServiceJourney>
      """
    );

    List<ValidationIssue> issues = validate(xml);

    assertThat(issues.size(), is(2));
  }

  @Test
  void testNewFormatWithReplacedJourneysShouldBeOk() {
    String xml = publicationDelivery(
      """
      <DatedServiceJourney version="1" id="TST:DatedServiceJourney:1">
        <ServiceJourneyRef ref="TST:ServiceJourney:1" version="1"/>
        <replacedJourneys>
          <DatedVehicleJourneyRef ref="TST:DatedServiceJourney:2"/>
        </replacedJourneys>
        <OperatingDayRef ref="TST:OperatingDay:1"/>
      </DatedServiceJourney>
      """
    );

    List<ValidationIssue> issues = validate(xml);

    assertThat(issues.size(), is(0));
  }

  @Test
  void testDatedServiceJourneyWithoutReplacementShouldBeOk() {
    String xml = publicationDelivery(
      """
      <DatedServiceJourney version="1" id="TST:DatedServiceJourney:1">
        <ServiceJourneyRef ref="TST:ServiceJourney:1" version="1"/>
        <OperatingDayRef ref="TST:OperatingDay:1"/>
      </DatedServiceJourney>
      """
    );

    List<ValidationIssue> issues = validate(xml);

    assertThat(issues.size(), is(0));
  }

  private List<ValidationIssue> validate(String xml) {
    XdmNode document = NETEX_XML_PARSER.parseByteArrayToXdmNode(xml.getBytes());
    XPathRuleValidationContext context = new XPathRuleValidationContext(
      document,
      NETEX_XML_PARSER,
      CODESPACE,
      "line.xml"
    );
    XPathRuleValidator validator = new XPathRuleValidator(
      new EnturTimetableDataValidationTreeFactory(
        new SimpleOrganisationAliasRepository(new HashSet<>()),
        new ValidationParametersConfig()
      )
    );
    return validator
      .validate(context)
      .stream()
      .filter(issue -> RULE_CODE.equals(issue.rule().code()))
      .toList();
  }

  private static String publicationDelivery(String datedServiceJourneys) {
    return (
      """
      <PublicationDelivery xmlns="http://www.netex.org.uk/netex" version="1">
        <dataObjects>
          <CompositeFrame id="TST:CompositeFrame:1" version="1">
            <codespaces>
              <Codespace id="tst">
                <Xmlns>TST</Xmlns>
                <XmlnsUrl>http://www.rutebanken.org/ns/tst</XmlnsUrl>
              </Codespace>
            </codespaces>
            <frames>
              <TimetableFrame id="TST:TimetableFrame:1" version="1">
                <vehicleJourneys>
                  """ +
      datedServiceJourneys +
      """
                </vehicleJourneys>
              </TimetableFrame>
            </frames>
          </CompositeFrame>
        </dataObjects>
      </PublicationDelivery>
      """
    );
  }
}
