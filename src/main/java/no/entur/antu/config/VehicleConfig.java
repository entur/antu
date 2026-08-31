package no.entur.antu.config;

import java.util.Set;
import no.entur.antu.validation.validator.vehicletype.DefaultVehicleRefRepository;
import no.entur.antu.validation.validator.vehicletype.VehicleRefRepository;
import no.entur.antu.validation.validator.vehicletype.VehicleReferenceResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VehicleConfig {

  @Bean
  @Profile("!test")
  VehicleReferenceResource vehicleReferenceResource(
    @Qualifier("vehicleRegistryWebClient") WebClient vehicleRegistryWebClient
  ) {
    return new VehicleReferenceResource(vehicleRegistryWebClient);
  }

  @Bean
  @Profile("!test")
  VehicleRefRepository vehicleRefRepository(
    VehicleReferenceResource vehicleReferenceResource,
    @Qualifier("vehicleReferenceCache") Set<String> vehicleReferenceCache
  ) {
    return new DefaultVehicleRefRepository(
      vehicleReferenceResource,
      vehicleReferenceCache
    );
  }
}
