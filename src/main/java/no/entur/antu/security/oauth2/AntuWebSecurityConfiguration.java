package no.entur.antu.security.oauth2;

import static org.springframework.security.config.Customizer.withDefaults;

import java.util.Arrays;
import org.entur.oauth2.multiissuer.MultiIssuerAuthenticationManagerResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Authentication and authorization configuration for Antu.
 * All requests must be authenticated except for the health and Actuator endpoints.
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
      // a JWT resource server has no use for sessions, and without this an unauthenticated 401
      // mints one per request via the saved-request cache
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(authz ->
        authz
          .requestMatchers(HttpMethod.GET, "/services/health")
          .permitAll()
          .requestMatchers("/actuator/prometheus")
          .permitAll()
          .requestMatchers("/actuator/health")
          .permitAll()
          .requestMatchers("/actuator/health/liveness")
          .permitAll()
          .requestMatchers("/actuator/health/readiness")
          .permitAll()
          .requestMatchers("/actuator/info")
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
