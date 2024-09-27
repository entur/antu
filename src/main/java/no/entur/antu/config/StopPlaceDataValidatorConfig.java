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
import no.entur.antu.validation.validator.id.NetexIdValidator;
import no.entur.antu.validation.validator.xpath.EnturStopPlaceDataValidationTreeFactory;
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
public class StopPlaceDataValidatorConfig {

  @Bean
  public ValidationTreeFactory stopPlaceDataValidationTreeFactory() {
    return new EnturStopPlaceDataValidationTreeFactory();
  }

  @Bean
  public XPathRuleValidator stopPlaceDataXPathValidator(
    @Qualifier(
      "stopPlaceDataValidationTreeFactory"
    ) ValidationTreeFactory validationTreeFactory,
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    return new XPathRuleValidator(
      validationTreeFactory,
      validationReportEntryFactory
    );
  }

  @Bean
  public NetexValidatorsRunner stopPlaceDataValidatorsRunner(
    NetexSchemaValidator netexSchemaValidator,
    @Qualifier("stopPlaceDataXPathValidator") XPathRuleValidator xpathValidator,
    NetexIdValidator netexIdValidator,
    VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
    VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator,
    ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
    NetexReferenceValidator netexReferenceValidator,
    NetexIdUniquenessValidator netexIdUniquenessValidator
  ) {
    List<XPathValidator> netexValidators = List.of(
      xpathValidator,
      netexIdValidator,
      versionOnLocalNetexIdValidator,
      versionOnRefToLocalNetexIdValidator,
      referenceToValidEntityTypeValidator,
      netexReferenceValidator,
      netexIdUniquenessValidator
    );
    NetexXMLParser netexXMLParser = new NetexXMLParser();
    return new NetexValidatorsRunner(
      netexXMLParser,
      netexSchemaValidator,
      netexValidators
    );
  }
}
