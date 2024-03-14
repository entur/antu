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
import no.entur.antu.validator.NetexValidatorsRunnerWithNetexEntitiesIndex;
import no.entur.antu.validator.id.NetexIdValidator;
import no.entur.antu.validator.journeypattern.stoppoint.samestoppoints.SameStopPoints;
import no.entur.antu.validator.journeypattern.stoppoint.stoppointscount.StopPointsCount;
import no.entur.antu.validator.passengerstopassignment.MissingPassengerStopAssignment;
import no.entur.antu.validator.servicejourney.passingtime.NonIncreasingPassingTime;
import no.entur.antu.validator.servicejourney.speed.UnexpectedSpeed;
import no.entur.antu.validator.servicejourney.transportmode.MismatchedTransportMode;
import no.entur.antu.validator.servicelink.InvalidServiceLinks;
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
  public MismatchedTransportMode mismatchedTransportMode(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new MismatchedTransportMode(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public NonIncreasingPassingTime nonIncreasingPassingTime(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new NonIncreasingPassingTime(validationReportEntryFactory);
  }

  @Bean
  public UnexpectedSpeed unexpectedSpeed(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new UnexpectedSpeed(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public MissingPassengerStopAssignment missingPassengerStopAssignment(
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
  public InvalidServiceLinks invalidServiceLinks(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new InvalidServiceLinks(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public SameStopPoints sameStopPoints(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new SameStopPoints(validationReportEntryFactory);
  }

  @Bean
  public StopPointsCount stopPointsCount(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new StopPointsCount(validationReportEntryFactory);
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
    MismatchedTransportMode mismatchedTransportMode,
    NonIncreasingPassingTime nonIncreasingPassingTime,
    UnexpectedSpeed unexpectedSpeed,
    MissingPassengerStopAssignment missingPassengerStopAssignment,
    InvalidServiceLinks invalidServiceLinks,
    SameStopPoints sameStopPoints,
    StopPointsCount stopPointsCount
  ) {
    List<NetexValidator> netexValidators = List.of(
      xpathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator,
      mismatchedTransportMode,
      nonIncreasingPassingTime,
      unexpectedSpeed,
      missingPassengerStopAssignment,
      invalidServiceLinks,
      sameStopPoints,
      stopPointsCount
    );
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));
    return new NetexValidatorsRunnerWithNetexEntitiesIndex(
      netexXMLParser,
      netexSchemaValidator,
      netexValidators
    );
  }
}
