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
import no.entur.antu.validator.authority.AuthorityIdValidator;
import no.entur.antu.validator.schema.NetexSchemaValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidatorConfig {

    @Bean("authorityIdValidator")
    public AuthorityIdValidator authorityIdValidator(OrganisationRepository organisationRepository) {
        return new AuthorityIdValidator(organisationRepository);
    }

    @Bean("netexSchemaValidator")
    public NetexSchemaValidator netexSchemaValidator(@Value("${antu.netex.validation.entries.max:100}") int maxValidationError) {
        return new NetexSchemaValidator(maxValidationError);
    }
}
