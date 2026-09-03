package no.entur.antu.config;

import java.util.Set;
import no.entur.antu.validation.validator.vehicletype.DefaultVehicleRefRepository;
import no.entur.antu.validation.validator.vehicletype.DummyVehicleRefRepository;
import no.entur.antu.validation.validator.vehicletype.VehicleRefRepository;
import no.entur.antu.validation.validator.vehicletype.VehicleReferenceResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class VehicleConfig {

  @Bean
  @Profile("!test")
  @ConditionalOnProperty(
          name = "antu.netex.validation.vehicles.enabled",
          havingValue = "true"
  )
  VehicleReferenceResource vehicleReferenceResource(
    @Qualifier("vehicleRegistryWebClient") WebClient vehicleRegistryWebClient
  ) {
    return new VehicleReferenceResource(vehicleRegistryWebClient);
  }

  @Bean
  @Profile("!test")
  @ConditionalOnProperty(
          name = "antu.netex.validation.vehicles.enabled",
          havingValue = "true"
  )
  VehicleRefRepository vehicleRefRepository(
    VehicleReferenceResource vehicleReferenceResource,
    @Qualifier("vehicleReferenceCache") Set<String> vehicleReferenceCache
  ) {
    return new DefaultVehicleRefRepository(
      vehicleReferenceResource,
      vehicleReferenceCache
    );
  }

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(
            name = "antu.netex.validation.vehicles.enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    VehicleRefRepository dummyVehicleRefRepository(
    ) {
        return new DummyVehicleRefRepository();
    }
}
