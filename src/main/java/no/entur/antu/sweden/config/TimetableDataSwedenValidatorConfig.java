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

package no.entur.antu.sweden.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import no.entur.antu.sweden.stop.RedisSwedenStopPlaceNetexIdRepository;
import no.entur.antu.sweden.stop.SwedenStopPlaceNetexIdRepository;
import no.entur.antu.sweden.stop.SwedenStopPlaceValidator;
import no.entur.antu.sweden.validator.EnturTimetableDataSwedenValidationTreeFactory;
import no.entur.antu.sweden.validator.LineRefOnGroupOfLinesIgnorer;
import no.entur.antu.sweden.validator.OrganisationRefOnStopPlaceIgnorer;
import no.entur.antu.validation.validator.id.NetexIdValidator;
import no.entur.antu.validation.validator.id.ReferenceToNsrValidator;
import org.entur.netex.validation.configuration.DefaultValidationConfigLoader;
import org.entur.netex.validation.configuration.ValidationConfigLoader;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.id.BlockJourneyReferencesIgnorer;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.entur.netex.validation.validator.id.NetexIdUniquenessValidator;
import org.entur.netex.validation.validator.id.NetexReferenceValidator;
import org.entur.netex.validation.validator.id.ReferenceToValidEntityTypeValidator;
import org.entur.netex.validation.validator.id.ServiceJourneyInterchangeIgnorer;
import org.entur.netex.validation.validator.id.VersionOnLocalNetexIdValidator;
import org.entur.netex.validation.validator.id.VersionOnRefToLocalNetexIdValidator;
import org.entur.netex.validation.validator.schema.NetexSchemaValidator;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathRuleValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for validating timetable data from Sweden.
 */
@Configuration
public class TimetableDataSwedenValidatorConfig {

  @Bean
  public ValidationConfigLoader swedenValidationConfigLoader(
    @Value(
      "${antu.netex.validation.configuration.file:configuration.antu.yaml}"
    ) String antuConfigurationFile,
    @Value(
      "${antu.netex.validation.configuration.file.sweden:configuration.antu.sweden.yaml}"
    ) String swedenConfigurationFile
  ) {
    return new DefaultValidationConfigLoader(
      List.of(antuConfigurationFile, swedenConfigurationFile)
    );
  }

  @Bean
  public ValidationReportEntryFactory swedenValidationReportEntryFactory(
    @Qualifier(
      "swedenValidationConfigLoader"
    ) ValidationConfigLoader validationConfigLoader
  ) {
    return new DefaultValidationEntryFactory(validationConfigLoader);
  }

  @Bean
  public ValidationTreeFactory swedenTimetableDataValidationTreeFactory() {
    return new EnturTimetableDataSwedenValidationTreeFactory();
  }

  @Bean
  public XPathRuleValidator swedenTimetableDataXPathValidator(
    @Qualifier(
      "swedenTimetableDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory,
    @Qualifier(
      "swedenValidationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new XPathRuleValidator(
      validationTreeFactory,
      validationReportEntryFactory
    );
  }

  @Bean
  public NetexReferenceValidator swedenNetexReferenceValidator(
    NetexIdRepository netexIdRepository,
    ReferenceToNsrValidator referenceToNsrValidator,
    @Qualifier(
      "swedenValidationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    List<ExternalReferenceValidator> externalReferenceValidators =
      new ArrayList<>();
    externalReferenceValidators.add(new BlockJourneyReferencesIgnorer());
    externalReferenceValidators.add(new ServiceJourneyInterchangeIgnorer());
    externalReferenceValidators.add(new OrganisationRefOnStopPlaceIgnorer());
    externalReferenceValidators.add(new LineRefOnGroupOfLinesIgnorer());
    externalReferenceValidators.add(referenceToNsrValidator);
    return new NetexReferenceValidator(
      netexIdRepository,
      externalReferenceValidators,
      validationReportEntryFactory
    );
  }

  @Bean
  public SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository(
    RedissonClient redissonClient
  ) {
    return new RedisSwedenStopPlaceNetexIdRepository(redissonClient);
  }

  @Bean
  public SwedenStopPlaceValidator swedenStopPlaceValidator(
    SwedenStopPlaceNetexIdRepository swedenStopPlaceNetexIdRepository,
    @Qualifier(
      "swedenValidationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new SwedenStopPlaceValidator(
      swedenStopPlaceNetexIdRepository,
      validationReportEntryFactory
    );
  }

  @Bean
  public NetexValidatorsRunner swedenTimetableDataSwedenValidatorsRunner(
    NetexSchemaValidator netexSchemaValidator,
    @Qualifier(
      "swedenTimetableDataXPathValidator"
    ) XPathRuleValidator swedenXPathValidator,
    NetexIdValidator netexIdValidator,
    VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
    VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator,
    ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
    SwedenStopPlaceValidator swedenStopPlaceValidator,
    @Qualifier(
      "swedenNetexReferenceValidator"
    ) NetexReferenceValidator swedenNetexReferenceValidator,
    NetexIdUniquenessValidator netexIdUniquenessValidator
  ) {
    List<XPathValidator> netexValidators = List.of(
      swedenXPathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      swedenStopPlaceValidator,
      swedenNetexReferenceValidator,
      netexIdUniquenessValidator
    );

    // ignore navigationPaths and equipments elements
    NetexXMLParser netexXMLParser = new NetexXMLParser(
      Set.of("navigationPaths", "equipments")
    );
    return NetexValidatorsRunner
      .of()
      .withNetexXMLParser(netexXMLParser)
      .withNetexSchemaValidator(netexSchemaValidator)
      .withXPathValidators(netexValidators)
      .build();
  }
}
