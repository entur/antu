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
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.NetexValidatorsRunnerWithNetexEntitiesIndex;
import no.entur.antu.validation.flex.validator.EnturFlexTimetableDataValidationTreeFactory;
import no.entur.antu.validation.flex.validator.EnturImportFlexTimetableDataValidationTreeFactory;
import no.entur.antu.validation.flex.validator.FileNameValidator;
import no.entur.antu.validation.flex.validator.flexiblearea.InvalidFlexibleAreaValidator;
import no.entur.antu.validation.validator.id.NetexIdValidator;
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

/**
 * Configuration for validating flexible transport timetable data.
 */
@Configuration
public class TimetableDataFlexValidatorConfig {

  @Bean
  public ValidationTreeFactory flexTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    return new EnturFlexTimetableDataValidationTreeFactory(
      organisationRepository
    );
  }

  @Bean
  public ValidationTreeFactory importFlexTimetableDataValidationTreeFactory(
    OrganisationRepository organisationRepository
  ) {
    return new EnturImportFlexTimetableDataValidationTreeFactory(
      organisationRepository
    );
  }

  @Bean
  public XPathValidator flexTimetableDataXPathValidator(
    @Qualifier(
      "flexTimetableDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory,
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new XPathValidator(
      validationTreeFactory,
      validationReportEntryFactory
    );
  }

  @Bean
  public XPathValidator importFlexTimetableDataXPathValidator(
    @Qualifier(
      "importFlexTimetableDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory,
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new XPathValidator(
      validationTreeFactory,
      validationReportEntryFactory
    );
  }

  @Bean
  public NetexIdValidator flexNetexIdValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    // TODO temporarily ignore unapproved codespace for Operator
    return new NetexIdValidator(
      validationReportEntryFactory,
      Set.of("Operator")
    );
  }

  @Bean
  public FileNameValidator fileNameValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new FileNameValidator(validationReportEntryFactory);
  }

  @Bean
  public InvalidFlexibleAreaValidator flexibleAreaValidator(
    @Qualifier(
      "validationReportEntryFactory"
    ) ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new InvalidFlexibleAreaValidator(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  /**
   * This validation runner is used for the flexible line data exported from Nplan.
   */
  @Bean
  public NetexValidatorsRunner flexTimetableDataValidatorsRunner(
    @Qualifier(
      "flexTimetableDataXPathValidator"
    ) XPathValidator flexXPathValidator,
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
    InvalidFlexibleAreaValidator invalidFlexibleAreaValidator
  ) {
    List<NetexValidator> netexValidators = List.of(
      fileNameValidator,
      flexXPathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator,
      invalidFlexibleAreaValidator
    );
    // do not ignore SiteFrame
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of());
    return new NetexValidatorsRunnerWithNetexEntitiesIndex(
      netexXMLParser,
      netexSchemaValidator,
      netexValidators
    );
  }

  /**
   * This validation runner is used for the flexible line data imported from operatørPortalen.
   */
  @Bean
  public NetexValidatorsRunner importFlexTimetableDataValidatorsRunner(
    @Qualifier(
      "importFlexTimetableDataXPathValidator"
    ) XPathValidator flexXPathValidator,
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
    InvalidFlexibleAreaValidator invalidFlexibleAreaValidator
  ) {
    List<NetexValidator> netexValidators = List.of(
      fileNameValidator,
      flexXPathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator,
      invalidFlexibleAreaValidator
    );
    // do not ignore SiteFrame
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of());
    return new NetexValidatorsRunnerWithNetexEntitiesIndex(
      netexXMLParser,
      netexSchemaValidator,
      netexValidators
    );
  }
}
