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
import no.entur.antu.netexdata.collectors.LineInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyInterchangeInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyStopsCollector;
import no.entur.antu.validation.validator.id.NetexIdValidator;
import no.entur.antu.validation.validator.interchange.alighting.InterchangeForAlightingAndBoardingValidator;
import no.entur.antu.validation.validator.interchange.distance.UnexpectedInterchangeDistanceValidator;
import no.entur.antu.validation.validator.interchange.duplicate.DuplicateInterchangesValidator;
import no.entur.antu.validation.validator.interchange.mandatoryfields.MandatoryFieldsValidator;
import no.entur.antu.validation.validator.interchange.stoppoints.StopPointsInVehicleJourneyValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceBetweenStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints.IdenticalStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref.SameQuayRefValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints.SameStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount.StopPointsCountValidator;
import no.entur.antu.validation.validator.line.DuplicateLineNameValidator;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
import no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeValidator;
import no.entur.antu.validation.validator.servicejourney.servicealteration.InvalidServiceAlterationValidator;
import no.entur.antu.validation.validator.servicejourney.servicealteration.MissingReplacementValidator;
import no.entur.antu.validation.validator.servicejourney.speed.UnexpectedSpeedValidator;
import no.entur.antu.validation.validator.servicejourney.transportmode.MismatchedTransportModeSubModeValidator;
import no.entur.antu.validation.validator.servicelink.distance.UnexpectedDistanceInServiceLinkValidator;
import no.entur.antu.validation.validator.servicelink.stoppoints.MismatchedStopPointsValidator;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.*;
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
    OrganisationAliasRepository organisationAliasRepository
  ) {
    return new EnturTimetableDataValidationTreeFactory(
      organisationAliasRepository
    );
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
  public InterchangeForAlightingAndBoardingValidator interchangeForAlightingAndBoardingValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    return new InterchangeForAlightingAndBoardingValidator(
      netexDataRepository,
      validationReportEntryFactory
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
    StopPointsInVehicleJourneyValidator stopPointsInVehicleJourneyValidator,
    InterchangeForAlightingAndBoardingValidator interchangeForAlightingAndBoardingValidator,
    DuplicateLineNameValidator duplicateLineNameValidator,
    LineInfoCollector lineInfoCollector,
    ServiceJourneyStopsCollector serviceJourneyStopsCollector,
    ServiceJourneyInterchangeInfoCollector serviceJourneyInterchangeInfoCollector,
    CommonDataRepositoryLoader commonDataRepository,
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
      new MismatchedTransportModeSubModeValidator(),
      new UnexpectedDistanceBetweenStopPointsValidator(),
      new IdenticalStopPointsValidator(),
      new SameQuayRefValidator(),
      new SameStopPointsValidator(),
      new StopPointsCountValidator(),
      new MissingPassengerStopAssignmentValidator(),
      new NonIncreasingPassingTimeValidator(),
      new UnexpectedSpeedValidator(),
      new UnexpectedDistanceInServiceLinkValidator(),
      new MismatchedStopPointsValidator(),
      new MandatoryFieldsValidator(),
      new DuplicateInterchangesValidator(),
      new InvalidServiceAlterationValidator(),
      new MissingReplacementValidator(),
      new UnexpectedInterchangeDistanceValidator()
    );

    List<DatasetValidator> netexTimetableDatasetValidators = List.of(
      duplicateLineNameValidator,
      stopPointsInVehicleJourneyValidator,
      interchangeForAlightingAndBoardingValidator
    );

    List<NetexDataCollector> commonDataCollectors = List.of(
      lineInfoCollector,
      serviceJourneyInterchangeInfoCollector,
      serviceJourneyStopsCollector
    );

    return NetexValidatorsRunner
      .of()
      .withNetexXMLParser(netexXMLParser)
      .withNetexSchemaValidator(netexSchemaValidator)
      .withXPathValidators(xPathValidators)
      .withJaxbValidators(jaxbValidators)
      .withDatasetValidators(netexTimetableDatasetValidators)
      .withNetexDataCollectors(commonDataCollectors)
      .withCommonDataRepository(commonDataRepository)
      .withNetexDataRepository(netexDataRepository)
      .withStopPlaceRepository(stopPlaceRepository)
      .withValidationReportEntryFactory(validationReportEntryFactory)
      .build();
  }
}
