package no.entur.antu.validation.validator.vehicletype;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;
import no.entur.antu.config.ValidatorConfig;
import no.entur.antu.validation.validator.id.ReferenceToNsrValidator;
import no.entur.antu.validation.validator.id.ReferenceToVehicleRegistryValidator;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.entur.netex.validation.validator.id.NetexReferenceValidator;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Test;

class VehicleTypeRefCheckTest {

  @Test
  void testServiceJourneyWithNonExistentVehicleTypeRefIsRejected() {
    var issues = doValidation(Set.of("NMR:VehicleType:1", "NMR:Vehicle:1"));
    // Assert that VehicleTypeRef produces a NETEX_ID_5 error
    assertTrue(
      issues
        .stream()
        .anyMatch(issue -> issue.rule().code().equals("NETEX_ID_5")),
      "VehicleTypeRef with non existing references should be rejected and produce NETEX_ID_5 errors"
    );
  }

  @Test
  void testServiceJourneyWithExistentVehicleTypeRefIsAccepted() {
    var issues = doValidation(Set.of("NMR:VehicleType:999", "NMR:Vehicle:123"));
    // Assert that VehicleTypeRef does not produce a NETEX_ID_5 error
    assertFalse(
      issues
        .stream()
        .anyMatch(issue -> issue.rule().code().equals("NETEX_ID_5")),
      "VehicleTypeRef with existing references should be accepted and don't produce NETEX_ID_5 errors"
    );
  }

  private List<ValidationIssue> doValidation(Set<String> allowedReferences) {
    // Create the test NeTEx XML with a ServiceJourney that has a non-existent VehicleTypeRef
    String netexXml =
      """
              <?xml version="1.0" encoding="UTF-8"?>
              <PublicationDelivery xmlns="http://www.netex.org.uk/netex">
                <dataObjects>
                  <CompositeFrame>
                    <frames>
                      <ServiceFrame>
                        <vehicleJourneys>
                          <ServiceJourney id="TST:ServiceJourney:1" version="1">
                            <VehicleTypeRef ref="NMR:VehicleType:999"/>
                            <VehicleRef ref="NMR:Vehicle:123"/>
                          </ServiceJourney>
                        </vehicleJourneys>
                      </ServiceFrame>
                    </frames>
                  </CompositeFrame>
                </dataObjects>
              </PublicationDelivery>
              """;

    // Setup the validator components
    NetexXMLParser parser = new NetexXMLParser(Set.of("SiteFrame"));
    NetexIdRepository netexIdRepository = mock(NetexIdRepository.class);
    StopPlaceRepository stopPlaceRepository = mock(StopPlaceRepository.class);
    ReferenceToNsrValidator referenceToNsrValidator =
      new ReferenceToNsrValidator(stopPlaceRepository);
    VehicleReferenceResource dummyResource = mock(
      VehicleReferenceResource.class
    );
    VehicleRefRepository vehicleRefRepository = new DefaultVehicleRefRepository(
      dummyResource,
      allowedReferences
    );
    ReferenceToVehicleRegistryValidator vehicleRegistryValidator =
      new ReferenceToVehicleRegistryValidator(vehicleRefRepository);

    ValidatorConfig config = new ValidatorConfig();
    NetexReferenceValidator validator = config.netexReferenceValidator(
      netexIdRepository,
      referenceToNsrValidator,
      vehicleRegistryValidator,
      true
    );

    // Parse the XML
    var document = parser.parseByteArrayToXdmNode(netexXml.getBytes());
    XPathValidationContext context = new XPathValidationContext(
      document,
      parser,
      "TST",
      null,
      Set.of(),
      List.of(
        new IdVersion(
          "NMR:VehicleType:999",
          null,
          "VehicleTypeRef",
          null,
          "Test.xml",
          99,
          34
        ),
        new IdVersion(
          "NMR:Vehicle:123",
          null,
          "VehicleRef",
          null,
          "Test.xml",
          100,
          24
        )
      ),
      "reportid-1"
    );

    // Run validation
    return validator.validate(context);
  }
}
