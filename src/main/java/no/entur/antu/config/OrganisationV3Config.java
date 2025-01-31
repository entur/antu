package no.entur.antu.config;

import no.entur.antu.organisation.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

@Configuration
public class OrganisationV3Config {

  @Bean
  @Profile("!test")
  OrganisationV3Resource organisationV3Resource(
    @Qualifier("orgRegisterV3Client") WebClient orgRegisterV3Client
  ) {
    return new OrganisationV3Resource(orgRegisterV3Client);
  }

  @Bean
  @Profile("!test")
  DefaultOrganisationV3Repository organisationV3Repository(
    OrganisationV3Resource organisationResource,
    @Qualifier("organisationIdCache") Set<String> organisationIdCache
  ) {
    return new DefaultOrganisationV3Repository(organisationResource, organisationIdCache);
  }
}
