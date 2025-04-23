package no.entur.antu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AgreementRegistryWebClientConfiguration {

    @Bean("agreementRegistryWebClient")
    @Profile("!test")
    WebClient agreementRegistryWebClient(@Value("${antu.agreement.registry.url}") String agreementRegistryUrl) {
        return WebClient.builder().defaultHeader("Et-Client-Name", "entur-antu").baseUrl(agreementRegistryUrl).build();
    }

}
