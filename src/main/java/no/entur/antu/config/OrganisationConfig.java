package no.entur.antu.config;

import no.entur.antu.organisation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

@Configuration
public class OrganisationConfig {

  @Bean
  @Profile("!test")
  OrganisationResource organisationResource(
    @Qualifier("orgRegisterV3Client") WebClient orgRegisterV3Client
  ) {
    return new OrganisationResource(orgRegisterV3Client);
  }

  @Bean
  @Profile("!test")
  DefaultOrganisationRepository organisationRepository(
    OrganisationResource organisationResource,
    @Qualifier("organisationIdCache") Set<String> organisationIdCache
  ) {
    return new DefaultOrganisationRepository(organisationResource, organisationIdCache);
  }
}
