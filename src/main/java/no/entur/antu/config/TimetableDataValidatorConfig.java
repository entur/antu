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
import no.entur.antu.commondata.CommonDataScraper;
import no.entur.antu.commondata.LineInfoScraper;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.NetexValidatorsRunnerWithNetexEntitiesIndex;
import no.entur.antu.validation.validator.id.NetexIdValidator;
import no.entur.antu.validation.validator.interchange.duplicate.DuplicateInterchangesValidator;
import no.entur.antu.validation.validator.interchange.mandatoryfields.MandatoryFieldsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceBetweenStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints.IdenticalStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref.SameQuayRefValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints.SameStopPointsValidator;
import no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount.StopPointsCountValidator;
import no.entur.antu.validation.validator.line.DuplicateLineNameValidator;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
import no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeValidator;
import no.entur.antu.validation.validator.servicejourney.speed.UnexpectedSpeedValidator;
import no.entur.antu.validation.validator.servicejourney.transportmode.MismatchedTransportModeValidator;
import no.entur.antu.validation.validator.servicelink.distance.UnexpectedDistanceInServiceLinkValidator;
import no.entur.antu.validation.validator.servicelink.stoppoints.MismatchedStopPointsValidator;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.validator.NetexDatasetValidator;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.XPathValidator;
import org.entur.netex.validation.validator.id.NetexIdUniquenessValidator;
import org.entur.netex.validation.validator.id.NetexReferenceValidator;
import org.entur.netex.validation.validator.id.ReferenceToValidEntityTypeValidator;
import org.entur.netex.validation.validator.id.VersionOnLocalNetexIdValidator;
import org.entur.netex.validation.validator.id.VersionOnRefToLocalNetexIdValidator;
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
    ) ValidationTreeFactory validationTreeFactory,
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new XPathRuleValidator(
      validationTreeFactory,
      validationReportEntryFactory
    );
  }

  @Bean
  public UnexpectedDistanceBetweenStopPointsValidator unexpectedDistanceBetweenStopPointsValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new UnexpectedDistanceBetweenStopPointsValidator(
      validationReportEntryFactory
    );
  }

  @Bean
  public IdenticalStopPointsValidator identicalStopPointsValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new IdenticalStopPointsValidator(validationReportEntryFactory);
  }

  @Bean
  public SameQuayRefValidator sameQuayRefValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new SameQuayRefValidator(validationReportEntryFactory);
  }

  @Bean
  public SameStopPointsValidator sameStopPointsValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new SameStopPointsValidator(validationReportEntryFactory);
  }

  @Bean
  public StopPointsCountValidator stopPointsCountValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new StopPointsCountValidator(validationReportEntryFactory);
  }

  @Bean
  public MissingPassengerStopAssignmentValidator missingPassengerStopAssignmentValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new MissingPassengerStopAssignmentValidator(
      validationReportEntryFactory
    );
  }

  @Bean
  public NonIncreasingPassingTimeValidator nonIncreasingPassingTimeValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new NonIncreasingPassingTimeValidator(validationReportEntryFactory);
  }

  @Bean
  public UnexpectedSpeedValidator unexpectedSpeedValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new UnexpectedSpeedValidator(validationReportEntryFactory);
  }

  @Bean
  public MismatchedTransportModeValidator mismatchedTransportModeValidator(
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
  public UnexpectedDistanceInServiceLinkValidator unexpectedDistanceInServiceLinkValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new UnexpectedDistanceInServiceLinkValidator(
      validationReportEntryFactory
    );
  }

  @Bean
  public MismatchedStopPointsValidator mismatchedStopPointsValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new MismatchedStopPointsValidator(validationReportEntryFactory);
  }

  @Bean
  public MandatoryFieldsValidator mandatoryFieldsValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new MandatoryFieldsValidator(validationReportEntryFactory);
  }

  @Bean
  public DuplicateInterchangesValidator duplicateInterchangesValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new DuplicateInterchangesValidator(validationReportEntryFactory);
  }

  @Bean
  public DuplicateLineNameValidator duplicateLineNameValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository
  ) {
    return new DuplicateLineNameValidator(
      validationReportEntryFactory,
      commonDataRepository
    );
  }

  @Bean
  public NetexValidatorsRunner timetableDataValidatorsRunner(
    NetexSchemaValidator netexSchemaValidator,
    @Qualifier("timetableDataXPathValidator") XPathRuleValidator xpathValidator,
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
    MismatchedTransportModeValidator mismatchedTransportModeValidator,
    UnexpectedDistanceInServiceLinkValidator unexpectedDistanceInServiceLinkValidator,
    MismatchedStopPointsValidator mismatchedStopPointsValidator,
    MandatoryFieldsValidator mandatoryFieldsValidator,
    DuplicateInterchangesValidator duplicateInterchangesValidator,
    DuplicateLineNameValidator duplicateLineNameValidator,
    LineInfoScraper lineInfoScraper,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));

    List<XPathValidator> netexTimetableDataValidators = List.of(
      xpathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator,
      unexpectedDistanceBetweenStopPointsValidator,
      identicalStopPointsValidator,
      sameQuayRefValidator,
      sameStopPointsValidator,
      stopPointsCountValidator,
      missingPassengerStopAssignmentValidator,
      nonIncreasingPassingTimeValidator,
      unexpectedSpeedValidator,
      mismatchedTransportModeValidator,
      unexpectedDistanceInServiceLinkValidator,
      mismatchedStopPointsValidator,
      mandatoryFieldsValidator,
      duplicateInterchangesValidator
    );

    List<NetexDatasetValidator> netexTimetableDatasetValidators = List.of(
      duplicateLineNameValidator
    );

    List<CommonDataScraper> commonDataScrapers = List.of(lineInfoScraper);

    return new NetexValidatorsRunnerWithNetexEntitiesIndex(
      netexXMLParser,
      netexSchemaValidator,
      netexTimetableDataValidators,
      netexTimetableDatasetValidators,
      commonDataScrapers,
      commonDataRepository,
      stopPlaceRepository
    );
  }
}
