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
import no.entur.antu.netexdata.collectors.DatedServiceJourneysCollector;
import no.entur.antu.netexdata.collectors.LineInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyDayTypesCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyInterchangeInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyStopsCollector;
import no.entur.antu.netexdata.collectors.activedatecollector.ActiveDatesCollector;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validation.validator.id.NetexIdValidator;
import no.entur.antu.validation.validator.interchange.distance.UnexpectedInterchangeDistanceValidator;
import no.entur.antu.validation.validator.interchange.duplicate.DuplicateInterchangesValidator;
import no.entur.antu.validation.validator.interchange.mandatoryfields.MandatoryFieldsValidator;
import no.entur.antu.validation.validator.interchange.stoppoints.StopPointsInVehicleJourneyValidator;
import no.entur.antu.validation.validator.interchange.waittime.UnexpectedWaitTimeAndActiveDatesValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceBetweenStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints.IdenticalStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref.SameQuayRefValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints.SameStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount.StopPointsCountValidator;
import no.entur.antu.validation.validator.line.DuplicateLineNameValidator;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
import no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeValidator;
import no.entur.antu.validation.validator.servicejourney.servicealteration.InvalidServiceAlterationValidator;
import no.entur.antu.validation.validator.servicejourney.servicealteration.MissingReplacementValidator;
import no.entur.antu.validation.validator.servicejourney.speed.UnexpectedSpeedValidator;
import no.entur.antu.validation.validator.servicelink.distance.UnexpectedDistanceInServiceLinkValidator;
import no.entur.antu.validation.validator.servicelink.stoppoints.MismatchedStopPointsValidator;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.DatasetValidator;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.XPathValidator;
import org.entur.netex.validation.validator.id.NetexIdUniquenessValidator;
import org.entur.netex.validation.validator.id.NetexReferenceValidator;
import org.entur.netex.validation.validator.id.ReferenceToValidEntityTypeValidator;
import org.entur.netex.validation.validator.id.VersionOnLocalNetexIdValidator;
import org.entur.netex.validation.validator.id.VersionOnRefToLocalNetexIdValidator;
import org.entur.netex.validation.validator.jaxb.CommonDataRepositoryLoader;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.schema.NetexSchemaValidator;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathRuleValidator;
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
  public XPathRuleValidator timetableDataXPathValidator(
    @Qualifier(
      "timetableDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory
  ) {
    return new XPathRuleValidator(validationTreeFactory);
  }

  @Bean
  public UnexpectedDistanceBetweenStopPointsValidator unexpectedDistanceBetweenStopPointsValidator() {
    return new UnexpectedDistanceBetweenStopPointsValidator();
  }

  @Bean
  public IdenticalStopPointsValidator identicalStopPointsValidator() {
    return new IdenticalStopPointsValidator();
  }

  @Bean
  public SameQuayRefValidator sameQuayRefValidator() {
    return new SameQuayRefValidator();
  }

  @Bean
  public SameStopPointsValidator sameStopPointsValidator() {
    return new SameStopPointsValidator();
  }

  @Bean
  public StopPointsCountValidator stopPointsCountValidator() {
    return new StopPointsCountValidator();
  }

  @Bean
  public MissingPassengerStopAssignmentValidator missingPassengerStopAssignmentValidator() {
    return new MissingPassengerStopAssignmentValidator();
  }

  @Bean
  public NonIncreasingPassingTimeValidator nonIncreasingPassingTimeValidator() {
    return new NonIncreasingPassingTimeValidator();
  }

  @Bean
  public UnexpectedSpeedValidator unexpectedSpeedValidator() {
    return new UnexpectedSpeedValidator();
  }

  @Bean
  public UnexpectedDistanceInServiceLinkValidator unexpectedDistanceInServiceLinkValidator() {
    return new UnexpectedDistanceInServiceLinkValidator();
  }

  @Bean
  public MismatchedStopPointsValidator mismatchedStopPointsValidator() {
    return new MismatchedStopPointsValidator();
  }

  @Bean
  public MandatoryFieldsValidator mandatoryFieldsValidator() {
    return new MandatoryFieldsValidator();
  }

  @Bean
  public DuplicateInterchangesValidator duplicateInterchangesValidator() {
    return new DuplicateInterchangesValidator();
  }

  @Bean
  public InvalidServiceAlterationValidator missingServiceAlterationValidator() {
    return new InvalidServiceAlterationValidator();
  }

  @Bean
  public MissingReplacementValidator missingReplacementValidator() {
    return new MissingReplacementValidator();
  }

  @Bean
  public UnexpectedInterchangeDistanceValidator unexpectedInterchangeDistanceValidator() {
    return new UnexpectedInterchangeDistanceValidator();
  }

  @Bean
  public StopPointsInVehicleJourneyValidator stopPointsInVehicleJourneyValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    return new StopPointsInVehicleJourneyValidator(
      validationReportEntryFactory,
      netexDataRepository
    );
  }

  @Bean
  public DuplicateLineNameValidator duplicateLineNameValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    return new DuplicateLineNameValidator(
      validationReportEntryFactory,
      netexDataRepository
    );
  }

  @Bean
  public UnexpectedWaitTimeAndActiveDatesValidator unexpectedWaitTimeValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    return new UnexpectedWaitTimeAndActiveDatesValidator(
      validationReportEntryFactory,
      netexDataRepository
    );
  }

  @Bean
  public NetexValidatorsRunner timetableDataValidatorsRunner(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    NetexSchemaValidator netexSchemaValidator,
    @Qualifier(
      "timetableDataXPathValidator"
    ) XPathRuleValidator xPathRuleValidator,
    NetexIdValidator netexIdValidator,
    VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
    VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator,
    ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
    NetexReferenceValidator netexReferenceValidator,
    @Qualifier(
      "netexIdUniquenessValidator"
    ) NetexIdUniquenessValidator netexIdUniquenessValidator,
    UnexpectedDistanceBetweenStopPointsValidator unexpectedDistanceBetweenStopPointsValidator,
    IdenticalStopPointsValidator identicalStopPointsValidator,
    SameQuayRefValidator sameQuayRefValidator,
    SameStopPointsValidator sameStopPointsValidator,
    StopPointsCountValidator stopPointsCountValidator,
    MissingPassengerStopAssignmentValidator missingPassengerStopAssignmentValidator,
    NonIncreasingPassingTimeValidator nonIncreasingPassingTimeValidator,
    UnexpectedSpeedValidator unexpectedSpeedValidator,
    UnexpectedDistanceInServiceLinkValidator unexpectedDistanceInServiceLinkValidator,
    MismatchedStopPointsValidator mismatchedStopPointsValidator,
    MandatoryFieldsValidator mandatoryFieldsValidator,
    DuplicateInterchangesValidator duplicateInterchangesValidator,
    InvalidServiceAlterationValidator invalidServiceAlterationValidator,
    UnexpectedInterchangeDistanceValidator unexpectedInterchangeDistanceValidator,
    StopPointsInVehicleJourneyValidator stopPointsInVehicleJourneyValidator,
    DuplicateLineNameValidator duplicateLineNameValidator,
    MissingReplacementValidator missingReplacementValidator,
    UnexpectedWaitTimeAndActiveDatesValidator unexpectedWaitTimeAndActiveDatesValidator,
    LineInfoCollector lineInfoCollector,
    ServiceJourneyStopsCollector serviceJourneyStopsCollector,
    ServiceJourneyInterchangeInfoCollector serviceJourneyInterchangeInfoCollector,
    ActiveDatesCollector activeDatesCollector,
    ServiceJourneyDayTypesCollector serviceJourneyDayTypesCollector,
    DatedServiceJourneysCollector datedServiceJourneysCollector,
    CommonDataRepositoryLoader commonDataRepositoryLoader,
    NetexDataRepository netexDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));

    List<XPathValidator> xPathValidators = List.of(
      xPathRuleValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator
    );

    List<JAXBValidator> jaxbValidators = List.of(
      unexpectedDistanceBetweenStopPointsValidator,
      identicalStopPointsValidator,
      sameQuayRefValidator,
      sameStopPointsValidator,
      stopPointsCountValidator,
      missingPassengerStopAssignmentValidator,
      nonIncreasingPassingTimeValidator,
      unexpectedSpeedValidator,
      unexpectedDistanceInServiceLinkValidator,
      mismatchedStopPointsValidator,
      mandatoryFieldsValidator,
      duplicateInterchangesValidator,
      invalidServiceAlterationValidator,
      missingReplacementValidator,
      duplicateInterchangesValidator,
      unexpectedInterchangeDistanceValidator
    );

    List<DatasetValidator> netexTimetableDatasetValidators = List.of(
      duplicateLineNameValidator,
      stopPointsInVehicleJourneyValidator,
      unexpectedWaitTimeAndActiveDatesValidator
    );

    List<NetexDataCollector> commonDataCollectors = List.of(
      lineInfoCollector,
      serviceJourneyInterchangeInfoCollector,
      serviceJourneyStopsCollector,
      activeDatesCollector,
      serviceJourneyDayTypesCollector,
      datedServiceJourneysCollector
    );

    return NetexValidatorsRunner
      .of()
      .withNetexXMLParser(netexXMLParser)
      .withNetexSchemaValidator(netexSchemaValidator)
      .withXPathValidators(xPathValidators)
      .withJaxbValidators(jaxbValidators)
      .withDatasetValidators(netexTimetableDatasetValidators)
      .withNetexDataCollectors(commonDataCollectors)
      .withCommonDataRepository(commonDataRepositoryLoader)
      .withNetexDataRepository(netexDataRepository)
      .withStopPlaceRepository(stopPlaceRepository)
      .withValidationReportEntryFactory(validationReportEntryFactory)
      .build();
  }
}
