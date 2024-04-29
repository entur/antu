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
import no.entur.antu.validation.NetexValidatorsRunnerWithNetexEntitiesIndex;
import no.entur.antu.validation.validator.id.NetexIdValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints.IdenticalStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref.SameQuayRefValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints.SameStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount.StopPointsCountValidator;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
import no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeValidator;
import no.entur.antu.validation.validator.servicejourney.speed.UnexpectedSpeedValidator;
import no.entur.antu.validation.validator.servicejourney.transportmode.MismatchedTransportModeValidator;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
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
  public MismatchedTransportModeValidator mismatchedTransportMode(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new MismatchedTransportModeValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public NonIncreasingPassingTimeValidator nonIncreasingPassingTime(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new NonIncreasingPassingTimeValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public UnexpectedSpeedValidator unexpectedSpeed(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new UnexpectedSpeedValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public MissingPassengerStopAssignmentValidator missingPassengerStopAssignment(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new MissingPassengerStopAssignmentValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public UnexpectedDistanceValidator unexpectedDistance(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new UnexpectedDistanceValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public SameStopPointsValidator sameStopPoints(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new SameStopPointsValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public SameQuayRefValidator sameQuayRefValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new SameQuayRefValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public IdenticalStopPointsValidator identicalStopPoints(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new IdenticalStopPointsValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public StopPointsCountValidator stopPointsCount(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new StopPointsCountValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Bean
  public no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceValidator unexpectedDistanceValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceValidator(
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
    MismatchedTransportModeValidator mismatchedTransportModeValidator,
    NonIncreasingPassingTimeValidator nonIncreasingPassingTimeValidator,
    UnexpectedSpeedValidator unexpectedSpeedValidator,
    MissingPassengerStopAssignmentValidator missingPassengerStopAssignmentValidator,
    UnexpectedDistanceValidator unexpectedDistance,
    SameStopPointsValidator sameStopPointsValidator,
    StopPointsCountValidator stopPointsCountValidator,
    no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceValidator unexpectedDistanceValidator,
    SameQuayRefValidator sameQuayRefValidator,
    IdenticalStopPointsValidator identicalStopPointsValidator
  ) {
    List<NetexValidator> netexValidators = List.of(
      xpathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator,
      mismatchedTransportModeValidator,
      nonIncreasingPassingTimeValidator,
      unexpectedSpeedValidator,
      missingPassengerStopAssignmentValidator,
      unexpectedDistance,
      sameStopPointsValidator,
      stopPointsCountValidator,
      unexpectedDistanceValidator,
      sameQuayRefValidator,
      identicalStopPointsValidator
    );
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));
    return new NetexValidatorsRunnerWithNetexEntitiesIndex(
      netexXMLParser,
      netexSchemaValidator,
      netexValidators
    );
  }
}
