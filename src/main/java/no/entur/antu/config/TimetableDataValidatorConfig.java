/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.antu.config;

import java.util.List;
import java.util.Set;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validator.NetexValidatorRunnerWithNetexEntitiesIndex;
import no.entur.antu.validator.id.NetexIdValidator;
import no.entur.antu.validator.servicejourney.passingtime.NonIncreasingPassingTime;
import no.entur.antu.validator.servicelinks.ServiceLinksValidator;
import no.entur.antu.validator.servicejourney.speed.SpeedValidator;
import no.entur.antu.validator.passengerstopassignment.MissingPassengerStopAssignment;
import no.entur.antu.validator.servicejourney.transportmode.TransportModeValidator;
import no.entur.antu.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.NetexValidator;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.id.NetexIdUniquenessValidator;
import org.entur.netex.validation.validator.id.NetexReferenceValidator;
import org.entur.netex.validation.validator.id.ReferenceToValidEntityTypeValidator;
import org.entur.netex.validation.validator.id.VersionOnLocalNetexIdValidator;
import org.entur.netex.validation.validator.id.VersionOnRefToLocalNetexIdValidator;
import org.entur.netex.validation.validator.schema.NetexSchemaValidator;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimetableDataValidatorConfig {

  @Bean
  public ValidationTreeFactory timetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    return new EnturTimetableDataValidationTreeFactory(organisationRepository);
  }

  @Bean
  public XPathValidator timetableDataXPathValidator(
    @Qualifier(
      "timetableDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory,
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new XPathValidator(
      validationTreeFactory,
      validationReportEntryFactory
    );
  }

  @Bean
  public TransportModeValidator transportModeValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new TransportModeValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public NonIncreasingPassingTime nonIncreasingPassingTimeValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new NonIncreasingPassingTime(validationReportEntryFactory);
  }

  @Bean
  public SpeedValidator speedValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new SpeedValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public MissingPassengerStopAssignment stopPointInJourneyPatternValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository
  ) {
    return new MissingPassengerStopAssignment(
      validationReportEntryFactory,
      commonDataRepository
    );
  }

  @Bean
  public ServiceLinksValidator serviceLinksValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new ServiceLinksValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public NetexValidatorsRunner timetableDataValidatorsRunner(
    NetexSchemaValidator netexSchemaValidator,
    @Qualifier("timetableDataXPathValidator") XPathValidator xpathValidator,
    NetexIdValidator netexIdValidator,
    VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
    VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator,
    ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
    NetexReferenceValidator netexReferenceValidator,
    @Qualifier(
      "netexIdUniquenessValidator"
    ) NetexIdUniquenessValidator netexIdUniquenessValidator,
    TransportModeValidator transportModeValidator,
    NonIncreasingPassingTime nonIncreasingPassingTime,
    SpeedValidator speedValidator,
    MissingPassengerStopAssignment missingPassengerStopAssignment,
    ServiceLinksValidator serviceLinksValidator
  ) {
    List<NetexValidator> netexValidators = List.of(
      xpathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator,
      transportModeValidator,
      nonIncreasingPassingTime,
      speedValidator,
      missingPassengerStopAssignment,
      serviceLinksValidator
    );
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));
    return new NetexValidatorRunnerWithNetexEntitiesIndex(
      netexXMLParser,
      netexSchemaValidator,
      netexValidators
    );
  }
}
