package no.entur.antu.config;

import no.entur.antu.validation.validator.organisation.AgreementResource;
import no.entur.antu.validation.validator.organisation.DefaultOrganisationAliasRepository;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

@Configuration
public class AgreementConfig {

  @Bean
  @Profile("!test")
  AgreementResource agreementResource(
    @Qualifier("agreementRegistryWebClient") WebClient agreementRegistryWebClient
  ) {
    return new AgreementResource(agreementRegistryWebClient);
  }

  @Bean
  @Profile("!test")
  OrganisationAliasRepository organisationAliasRepository(
    AgreementResource agreementResource,
    @Qualifier("organisationAliasCache") Set<String> organisationAliasCache
  ) {
    return new DefaultOrganisationAliasRepository(
      agreementResource,
      organisationAliasCache
    );
  }
}
