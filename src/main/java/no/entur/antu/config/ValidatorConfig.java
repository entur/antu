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

import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validator.id.NetexIdValidator;
import no.entur.antu.validator.id.ReferenceToNsrValidator;
import no.entur.antu.validator.id.TrainElementRegistryIdValidator;
import no.entur.antu.validator.xpath.EnturValidationTreeFactory;
import org.entur.netex.validation.validator.NetexValidator;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.id.BlockJourneyReferencesIgnorer;
import org.entur.netex.validation.validator.id.ExternalReferenceValidator;
import org.entur.netex.validation.validator.id.NeTexReferenceValidator;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.entur.netex.validation.validator.id.NetexIdUniquenessValidator;
import org.entur.netex.validation.validator.id.ReferenceToValidEntityTypeValidator;
import org.entur.netex.validation.validator.id.ServiceJourneyInterchangeIgnorer;
import org.entur.netex.validation.validator.id.VersionOnLocalNetexIdValidator;
import org.entur.netex.validation.validator.id.VersionOnRefToLocalNetexIdValidator;
import org.entur.netex.validation.validator.schema.NetexSchemaValidator;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ValidatorConfig {

    @Bean("netexSchemaValidator")
    public NetexSchemaValidator netexSchemaValidator(@Value("${antu.netex.validation.entries.max:100}") int maxValidationError) {
        return new NetexSchemaValidator(maxValidationError);
    }

    @Bean()
    public ValidationTreeFactory validationTreeFactory(OrganisationRepository organisationRepository) {
        return new EnturValidationTreeFactory(organisationRepository);
    }

    @Bean("xpathValidator")
    public XPathValidator xpathValidator(ValidationTreeFactory validationTreeFactory) {
        return new XPathValidator(validationTreeFactory);
    }

    @Bean("netexIdValidator")
    public NetexIdValidator netexIdValidator() {
        return new NetexIdValidator();
    }

    @Bean("versionOnLocalNetexIdValidator")
    public VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator() {
        return new VersionOnLocalNetexIdValidator();
    }

    @Bean("versionOnRefToLocalNetexIdValidator")
    public VersionOnRefToLocalNetexIdValidator versionOnRefToLocalNetexIdValidator() {
        return new VersionOnRefToLocalNetexIdValidator();
    }

    @Bean("refToValidEntityTypeValidator")
    public ReferenceToValidEntityTypeValidator refToValidEntityTypeValidator() {
        return new ReferenceToValidEntityTypeValidator();
    }

    @Bean("nsrRefValidator")
    public ReferenceToNsrValidator referenceToNsrValidator(StopPlaceRepository stopPlaceRepository) {
        return new ReferenceToNsrValidator(stopPlaceRepository);
    }

    @Bean("neTexReferenceValidator")
    public NeTexReferenceValidator neTexReferenceValidator(NetexIdRepository netexIdRepository, ReferenceToNsrValidator referenceToNsrValidator) {
        List<ExternalReferenceValidator> externalReferenceValidators = new ArrayList<>();
        externalReferenceValidators.add(new BlockJourneyReferencesIgnorer());
        externalReferenceValidators.add(new ServiceJourneyInterchangeIgnorer());
        externalReferenceValidators.add(new TrainElementRegistryIdValidator());
        externalReferenceValidators.add(referenceToNsrValidator);
        return new NeTexReferenceValidator(netexIdRepository, externalReferenceValidators);
    }

    @Bean("netexIdUniquenessValidator")
    public NetexIdUniquenessValidator netexIdUniquenessValidator(NetexIdRepository netexIdRepository) {
        return new NetexIdUniquenessValidator(netexIdRepository);
    }

    @Bean("netexValidatorsRunner")
    public NetexValidatorsRunner netexValidatorsRunner(NetexSchemaValidator netexSchemaValidator,
                                                       XPathValidator xpathValidator,
                                                       NetexIdValidator netexIdValidator,
                                                       VersionOnLocalNetexIdValidator versionOnLocalNetexIdValidator,
                                                       ReferenceToValidEntityTypeValidator referenceToValidEntityTypeValidator,
                                                       NeTexReferenceValidator neTexReferenceValidator,
                                                       NetexIdUniquenessValidator netexIdUniquenessValidator) {
        List<NetexValidator> netexValidators = List.of(xpathValidator, netexIdValidator, versionOnLocalNetexIdValidator, referenceToValidEntityTypeValidator, neTexReferenceValidator, netexIdUniquenessValidator);
        return new NetexValidatorsRunner(netexSchemaValidator, netexValidators);
    }


}
