package no.entur.antu.config;

import static no.entur.antu.Constants.ET_CLIENT_NAME_HEADER;
import static no.entur.antu.Constants.ET_CLIENT_NAME_HEADER_VALUE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VehicleRegistryWebClientConfiguration {

  @Bean("vehicleRegistryWebClient")
  @Profile("!test")
  WebClient vehicleRegistryWebClient(
    @Value("${antu.vehicle.registry.url}") String vehicleRegistryUrl
  ) {
    return WebClient
      .builder()
      .defaultHeader(ET_CLIENT_NAME_HEADER, ET_CLIENT_NAME_HEADER_VALUE)
      .baseUrl(vehicleRegistryUrl)
      .build();
  }
}
