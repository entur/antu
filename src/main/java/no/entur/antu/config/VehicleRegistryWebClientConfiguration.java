package no.entur.antu.config;

import static no.entur.antu.Constants.ET_CLIENT_NAME_HEADER;
import static no.entur.antu.Constants.ET_CLIENT_NAME_HEADER_VALUE;

import org.entur.oauth2.AuthorizedWebClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VehicleRegistryWebClientConfiguration {

  @Bean("vehicleRegistryWebClient")
  @Profile("!test")
  WebClient vehicleRegistryWebClient(
    @Value("${antu.vehicle.registry.url}") String vehicleRegistryUrl,
    WebClient.Builder webClientBuilder,
    OAuth2ClientProperties properties,
    @Value("${ror.oauth2.client.audience.sobek}") String audience
  ) {
    return new AuthorizedWebClientBuilder(
      webClientBuilder
        .baseUrl(vehicleRegistryUrl)
        .defaultHeader(ET_CLIENT_NAME_HEADER, ET_CLIENT_NAME_HEADER_VALUE)
    )
      .withOAuth2ClientProperties(properties)
      .withAudience(audience)
      .withClientRegistrationId("sobek")
      .build();
  }
}
