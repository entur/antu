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
import no.entur.antu.validator.NetexValidator;
import no.entur.antu.validator.NetexValidatorsRunner;
import no.entur.antu.validator.id.BlockJourneyReferencesIgnorer;
import no.entur.antu.validator.id.CommonNetexIdRepository;
import no.entur.antu.validator.id.ExternalReferenceValidator;
import no.entur.antu.validator.id.NeTexReferenceValidator;
import no.entur.antu.validator.id.NetexIdRepository;
import no.entur.antu.validator.id.NetexIdUniquenessValidator;
import no.entur.antu.validator.id.NetexIdValidator;
import no.entur.antu.validator.id.ReferenceToNsrValidator;
import no.entur.antu.validator.id.ReferenceToValidEntityTypeValidator;
import no.entur.antu.validator.id.ServiceJourneyInterchangeIgnorer;
import no.entur.antu.validator.id.TrainElementRegistryIdValidator;
import no.entur.antu.validator.id.VersionOnLocalNetexIdValidator;
import no.entur.antu.validator.id.VersionOnRefToLocalNetexIdValidator;
import no.entur.antu.validator.schema.NetexSchemaValidator;
import no.entur.antu.validator.xpath.XPathValidator;
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

    @Bean("xpathValidator")
    public XPathValidator xpathValidator(OrganisationRepository organisationRepository) {
        return new XPathValidator(organisationRepository);
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
    public NeTexReferenceValidator neTexReferenceValidator(CommonNetexIdRepository commonNetexIdRepository, ReferenceToNsrValidator referenceToNsrValidator) {
        List<ExternalReferenceValidator> externalReferenceValidators = new ArrayList<>();
        externalReferenceValidators.add(new BlockJourneyReferencesIgnorer());
        externalReferenceValidators.add(new ServiceJourneyInterchangeIgnorer());
        externalReferenceValidators.add(new TrainElementRegistryIdValidator());
        externalReferenceValidators.add(referenceToNsrValidator);
        return new NeTexReferenceValidator(commonNetexIdRepository, externalReferenceValidators);
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
