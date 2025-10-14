package no.entur.antu.config;

import java.util.Set;
import no.entur.antu.validation.NetexCodespace;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "antu.validation")
public class ValidationParametersConfig {

  private Set<NetexCodespace> additionalAllowedCodespaces;

  public Set<NetexCodespace> getAdditionalAllowedCodespaces() {
    return additionalAllowedCodespaces;
  }

  public void setAdditionalAllowedCodespaces(
    Set<NetexCodespace> additionalAllowedCodespaces
  ) {
    this.additionalAllowedCodespaces = additionalAllowedCodespaces;
  }
}
