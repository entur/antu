package no.entur.antu.config.cache;

import static no.entur.antu.config.cache.CacheConfig.*;

import java.util.Map;
import no.entur.antu.routes.validation.DefaultValidationStateRepository;
import no.entur.antu.routes.validation.ValidationStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationStateConfig {

  @Bean
  public ValidationStateRepository validationStateRepository(
    @Qualifier(
      VALIDATION_STATE_CACHE
    ) Map<String, ValidationState> validationStates
  ) {
    return new DefaultValidationStateRepository(validationStates);
  }
}
