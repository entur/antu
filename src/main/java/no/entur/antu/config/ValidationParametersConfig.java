package no.entur.antu.config;

import no.entur.antu.validation.NetexCodespace;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "antu.validation")
public class ValidationParametersConfig {
    public Set<NetexCodespace> additionalAllowedCodespaces;
}
