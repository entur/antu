package no.entur.antu.config;

import static no.entur.antu.Constants.ORGANISATION_ET_CLIENT_NAME;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AgreementRegistryWebClientConfiguration {

  @Bean("agreementRegistryWebClient")
  @Profile("!test")
  WebClient agreementRegistryWebClient(
    @Value("${antu.agreement.registry.url}") String agreementRegistryUrl
  ) {
    return WebClient
      .builder()
      .defaultHeader("Et-Client-Name", ORGANISATION_ET_CLIENT_NAME)
      .baseUrl(agreementRegistryUrl)
      .build();
  }
}
