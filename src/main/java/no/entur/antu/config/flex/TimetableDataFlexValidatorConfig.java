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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import no.entur.antu.config.ValidationParametersConfig;
import no.entur.antu.validation.flex.validator.EnturFlexTimetableDataValidationTreeFactory;
import no.entur.antu.validation.flex.validator.EnturImportFlexTimetableDataValidationTreeFactory;
import no.entur.antu.validation.flex.validator.FileNameValidator;
import no.entur.antu.validation.flex.validator.flexiblearea.InvalidFlexibleAreaValidator;
import no.entur.antu.validation.validator.id.NetexIdValidator;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeValidator;
import no.entur.antu.validation.validator.servicejourney.transportmode.MismatchedTransportModeSubModeValidator;
import org.entur.netex.validation.configuration.DefaultValidationConfigLoader;
import org.entur.netex.validation.configuration.ValidationConfigLoader;
import org.entur.netex.validation.validator.DefaultValidationEntryFactory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for validating flexible transport timetable data.
 */
@Configuration
public class TimetableDataFlexValidatorConfig {

  @Bean
  public ValidationTreeFactory flexTimetableDataValidationTreeFactory(
    OrganisationAliasRepository organisationAliasRepository,
    ValidationParametersConfig validationParametersConfig
  ) {
    return new EnturFlexTimetableDataValidationTreeFactory(
      organisationAliasRepository,
      validationParametersConfig
    );
  }

  @Bean
  public ValidationConfigLoader flexValidationConfigLoader(
    @Value(
      "${antu.netex.validation.configuration.file:configuration.antu.yaml}"
    ) String antuConfigurationFile,
    @Value(
      "${antu.netex.validation.configuration.file.flex:configuration.antu.flex.yaml}"
    ) String flexConfigurationFile
  ) {
    return new DefaultValidationConfigLoader(
      List.of(antuConfigurationFile, flexConfigurationFile)
    );
  }

  @Bean
  public ValidationReportEntryFactory flexValidationReportEntryFactory(
    @Qualifier(
      "flexValidationConfigLoader"
    ) ValidationConfigLoader validationConfigLoader
  ) {
    return new DefaultValidationEntryFactory(validationConfigLoader);
  }

  @Bean
  public ValidationTreeFactory importFlexTimetableDataValidationTreeFactory(
    OrganisationAliasRepository organisationAliasRepository,
    ValidationParametersConfig validationParametersConfig
  ) {
    return new EnturImportFlexTimetableDataValidationTreeFactory(
      organisationAliasRepository,
      validationParametersConfig
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
  public NetexIdValidator flexNetexIdValidator(ValidationParametersConfig validationParametersConfig) {
    // TODO temporarily ignore unapproved codespace for Operator
    return new NetexIdValidator(Set.of("Operator"), validationParametersConfig.getAdditionalAllowedCodespaces());
  }

  @Bean
  public FileNameValidator fileNameValidator() {
    return new FileNameValidator();
  }

  @Bean
  public InvalidFlexibleAreaValidator flexibleAreaValidator() {
    return new InvalidFlexibleAreaValidator();
  }

  @Bean
  public List<XPathValidator> commonXPathValidators(
    FileNameValidator fileNameValidator,
    @Qualifier("flexNetexIdValidator") NetexIdValidator netexIdValidator,
    VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
    VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator,
    ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
    NetexReferenceValidator netexReferenceValidator,
    @Qualifier(
      "netexIdUniquenessValidator"
    ) NetexIdUniquenessValidator netexIdUniquenessValidator
  ) {
    return List.of(
      fileNameValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator
    );
  }

  private List<XPathValidator> mergeXPathValidators(
    XPathValidator validator,
    List<XPathValidator> validators
  ) {
    ArrayList<XPathValidator> listOfValidators = new ArrayList<>(validators);
    listOfValidators.add(validator);
    return listOfValidators;
  }

  @Bean
  public List<XPathValidator> importFlexTimetableDataXPathValidators(
    List<XPathValidator> commonXPathValidators,
    @Qualifier(
      "importFlexTimetableDataXPathValidator"
    ) XPathRuleValidator flexXPathValidator
  ) {
    return mergeXPathValidators(flexXPathValidator, commonXPathValidators);
  }

  @Bean
  public List<XPathValidator> flexTimetableDataXPathValidators(
    List<XPathValidator> commonXPathValidators,
    @Qualifier(
      "flexTimetableDataXPathValidator"
    ) XPathRuleValidator flexXPathValidator
  ) {
    return mergeXPathValidators(flexXPathValidator, commonXPathValidators);
  }

  /**
   * This validation runner is used for the flexible line data exported from Nplan.
   */
  @Bean
  public NetexValidatorsRunner flexTimetableDataValidatorsRunner(
    @Qualifier(
      "flexValidationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    NetexSchemaValidator netexSchemaValidator,
    List<XPathValidator> flexTimetableDataXPathValidators,
    InvalidFlexibleAreaValidator invalidFlexibleAreaValidator,
    CommonDataRepositoryLoader commonDataRepository,
    NetexDataRepository netexDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    List<JAXBValidator> jaxbValidators = List.of(
      invalidFlexibleAreaValidator,
      new MismatchedTransportModeSubModeValidator(),
      new NonIncreasingPassingTimeValidator()
    );
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of());
    return NetexValidatorsRunner
      .of()
      .withNetexXMLParser(netexXMLParser)
      .withNetexSchemaValidator(netexSchemaValidator)
      .withXPathValidators(flexTimetableDataXPathValidators)
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
      "flexTimetableDataValidatorsRunner"
    ) NetexValidatorsRunner baseRunner,
    List<XPathValidator> importFlexTimetableDataXPathValidators
  ) {
    return baseRunner
      .toBuilder()
      .withXPathValidators(importFlexTimetableDataXPathValidators)
      .build();
  }
}
