package no.entur.antu.security.oauth2;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;
import org.entur.oauth2.multiissuer.MultiIssuerAuthenticationManagerResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Authentication and authorization configuration for Antu.
 * All requests must be authenticated except for the Swagger and Actuator endpoints.
 */
@Profile("!test")
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class AntuWebSecurityConfiguration {

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedHeaders(
      Arrays.asList(
        "Origin",
        "Accept",
        "X-Requested-With",
        "Content-Type",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers",
        "Authorization",
        "x-correlation-id",
        "baggage",
        "sentry-trace",
        "et-client-name"
      )
    );
    configuration.addAllowedOrigin("*");
    configuration.setAllowedMethods(
      Arrays.asList("GET", "PUT", "POST", "DELETE")
    );
    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(
    HttpSecurity http,
    MultiIssuerAuthenticationManagerResolver multiIssuerAuthenticationManagerResolver
  ) throws Exception {
    http
      .cors(withDefaults())
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(authz ->
        authz
          .requestMatchers(
            AntPathRequestMatcher.antMatcher(
              "/services/validation-report/swagger.json"
            )
          )
          .permitAll()
          .requestMatchers(
            AntPathRequestMatcher.antMatcher("/services/swagger.json")
          )
          .permitAll()
          .requestMatchers(
            AntPathRequestMatcher.antMatcher("/actuator/prometheus")
          )
          .permitAll()
          .requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/health"))
          .permitAll()
          .requestMatchers(
            AntPathRequestMatcher.antMatcher("/actuator/health/liveness")
          )
          .permitAll()
          .requestMatchers(
            AntPathRequestMatcher.antMatcher("/actuator/health/readiness")
          )
          .permitAll()
          .requestMatchers(AntPathRequestMatcher.antMatcher("/actuator/info"))
          .permitAll()
          .anyRequest()
          .authenticated()
      )
      .oauth2ResourceServer(configurer ->
        configurer.authenticationManagerResolver(
          multiIssuerAuthenticationManagerResolver
        )
      )
      .oauth2Client(withDefaults());
    return http.build();
  }
}
