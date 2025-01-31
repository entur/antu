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

package no.entur.antu.config.flex;

import java.util.List;
import java.util.Set;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.organisation.OrganisationV3Repository;
import no.entur.antu.validation.flex.validator.EnturFlexTimetableDataValidationTreeFactory;
import no.entur.antu.validation.flex.validator.EnturImportFlexTimetableDataValidationTreeFactory;
import no.entur.antu.validation.flex.validator.FileNameValidator;
import no.entur.antu.validation.flex.validator.flexiblearea.InvalidFlexibleAreaValidator;
import no.entur.antu.validation.validator.id.NetexIdValidator;
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
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.schema.NetexSchemaValidator;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathRuleValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for validating flexible transport timetable data.
 */
@Configuration
public class TimetableDataFlexValidatorConfig {

  @Bean
  public ValidationTreeFactory flexTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository,
    OrganisationV3Repository organisationV3Repository
  ) {
    return new EnturFlexTimetableDataValidationTreeFactory(
      organisationRepository,
      organisationV3Repository
    );
  }

  @Bean
  public ValidationTreeFactory importFlexTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository,
    OrganisationV3Repository organisationV3Repository
  ) {
    return new EnturImportFlexTimetableDataValidationTreeFactory(
      organisationRepository,
      organisationV3Repository
    );
  }

  @Bean
  public XPathRuleValidator flexTimetableDataXPathValidator(
    @Qualifier(
      "flexTimetableDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory
  ) {
    return new XPathRuleValidator(validationTreeFactory);
  }

  @Bean
  public XPathRuleValidator importFlexTimetableDataXPathValidator(
    @Qualifier(
      "importFlexTimetableDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory
  ) {
    return new XPathRuleValidator(validationTreeFactory);
  }

  @Bean
  public NetexIdValidator flexNetexIdValidator() {
    // TODO temporarily ignore unapproved codespace for Operator
    return new NetexIdValidator(Set.of("Operator"));
  }

  @Bean
  public FileNameValidator fileNameValidator() {
    return new FileNameValidator();
  }

  @Bean
  public InvalidFlexibleAreaValidator flexibleAreaValidator() {
    return new InvalidFlexibleAreaValidator();
  }

  /**
   * This validation runner is used for the flexible line data exported from Nplan.
   */
  @Bean
  public NetexValidatorsRunner flexTimetableDataValidatorsRunner(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    @Qualifier(
      "flexTimetableDataXPathValidator"
    ) XPathRuleValidator flexXPathValidator,
    @Qualifier("flexNetexIdValidator") NetexIdValidator netexIdValidator,
    @Qualifier(
      "netexIdUniquenessValidator"
    ) NetexIdUniquenessValidator netexIdUniquenessValidator,
    NetexSchemaValidator netexSchemaValidator,
    VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
    VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator,
    ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
    NetexReferenceValidator netexReferenceValidator,
    FileNameValidator fileNameValidator,
    InvalidFlexibleAreaValidator invalidFlexibleAreaValidator,
    CommonDataRepositoryLoader commonDataRepository,
    NetexDataRepository netexDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    List<XPathValidator> xpathValidators = List.of(
      fileNameValidator,
      flexXPathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator
    );
    List<JAXBValidator> jaxbValidators = List.of(invalidFlexibleAreaValidator);
    // do not ignore SiteFrame
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of());
    return NetexValidatorsRunner
      .of()
      .withNetexXMLParser(netexXMLParser)
      .withNetexSchemaValidator(netexSchemaValidator)
      .withXPathValidators(xpathValidators)
      .withJaxbValidators(jaxbValidators)
      .withCommonDataRepository(commonDataRepository)
      .withNetexDataRepository(netexDataRepository)
      .withStopPlaceRepository(stopPlaceRepository)
      .withValidationReportEntryFactory(validationReportEntryFactory)
      .build();
  }

  /**
   * This validation runner is used for the flexible line data imported from operatørPortalen.
   */
  @Bean
  public NetexValidatorsRunner importFlexTimetableDataValidatorsRunner(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    @Qualifier(
      "importFlexTimetableDataXPathValidator"
    ) XPathRuleValidator flexXPathValidator,
    @Qualifier("flexNetexIdValidator") NetexIdValidator netexIdValidator,
    @Qualifier(
      "netexIdUniquenessValidator"
    ) NetexIdUniquenessValidator netexIdUniquenessValidator,
    NetexSchemaValidator netexSchemaValidator,
    VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
    VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator,
    ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
    NetexReferenceValidator netexReferenceValidator,
    FileNameValidator fileNameValidator,
    InvalidFlexibleAreaValidator invalidFlexibleAreaValidator,
    CommonDataRepositoryLoader commonDataRepository,
    NetexDataRepository netexDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    List<XPathValidator> xpathValidators = List.of(
      fileNameValidator,
      flexXPathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator
    );

    List<JAXBValidator> jaxbValidators = List.of(invalidFlexibleAreaValidator);

    // do not ignore SiteFrame
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of());
    return NetexValidatorsRunner
      .of()
      .withNetexXMLParser(netexXMLParser)
      .withNetexSchemaValidator(netexSchemaValidator)
      .withXPathValidators(xpathValidators)
      .withJaxbValidators(jaxbValidators)
      .withCommonDataRepository(commonDataRepository)
      .withNetexDataRepository(netexDataRepository)
      .withStopPlaceRepository(stopPlaceRepository)
      .withValidationReportEntryFactory(validationReportEntryFactory)
      .build();
  }
}
