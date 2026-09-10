package no.entur.antu.config;

import static no.entur.antu.Constants.ET_CLIENT_NAME_HEADER;
import static no.entur.antu.Constants.ET_CLIENT_NAME_HEADER_VALUE;

import org.entur.oauth2.AuthorizedWebClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VehicleRegistryWebClientConfiguration {

  @Value("${antu.vehicle.registry.max-in-memory-size:500KB}")
  DataSize maxInMemorySize;

  @Bean("vehicleRegistryWebClient")
  @Profile("!test")
  @ConditionalOnProperty(
    name = "antu.netex.validation.vehicles.enabled",
    havingValue = "true"
  )
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
        .exchangeStrategies(
          ExchangeStrategies
            .builder()
            .codecs(codecs ->
              codecs
                .defaultCodecs()
                .maxInMemorySize((int) maxInMemorySize.toBytes())
            )
            .build()
        )
    )
      .withOAuth2ClientProperties(properties)
      .withAudience(audience)
      .withClientRegistrationId("sobek")
      .build();
  }
}
