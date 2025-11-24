/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.antu.routes.rest;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_REPORTS;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.config.Customizer.withDefaults;

import com.nimbusds.jose.JWSAlgorithm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import no.entur.antu.AntuRouteBuilderIntegrationTestBase;
import no.entur.antu.TestApp;
import no.entur.antu.security.AntuAuthorizationService;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.organisation.authorization.AuthorizationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
  classes = TestApp.class
)
class RestValidationReportRouteBuilderIntegrationTest
  extends AntuRouteBuilderIntegrationTestBase {

  private static final String TEST_CODESPACE = "TST";
  private static final String TEST_VALIDATION_REPORT_ID = "test-report-123";

  @TestConfiguration
  @EnableWebSecurity
  static class RestValidationReportRouteBuilderTestContextConfiguration {

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
          "x-correlation-id"
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
    @ConditionalOnWebApplication
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
      http
        .cors(withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authz ->
          authz
            .requestMatchers(
              AntPathRequestMatcher.antMatcher("/services/openapi.json")
            )
            .permitAll()
            .requestMatchers(
              AntPathRequestMatcher.antMatcher("/actuator/prometheus")
            )
            .permitAll()
            .requestMatchers(
              AntPathRequestMatcher.antMatcher("/actuator/health")
            )
            .permitAll()
            .requestMatchers(
              AntPathRequestMatcher.antMatcher("/actuator/health/liveness")
            )
            .permitAll()
            .requestMatchers(
              AntPathRequestMatcher.antMatcher("/actuator/health/readiness")
            )
            .permitAll()
            .anyRequest()
            .authenticated()
        )
        .oauth2ResourceServer(configurer -> configurer.jwt(withDefaults()))
        .oauth2Client(withDefaults());
      return http.build();
    }

    @Bean
    public JwtDecoder jwtdecoder() {
      return token -> createTestJwtToken();
    }

    private Jwt createTestJwtToken() {
      String userId = "test-user";
      return Jwt
        .withTokenValue("test-token")
        .header("typ", "JWT")
        .header("alg", JWSAlgorithm.RS256.getName())
        .claim("iss", "https://test-issuer.entur.org")
        .claim("scope", "openid profile email")
        .subject(userId)
        .audience(Set.of("test-audience"))
        .build();
    }

    @Bean
    public AuthorizationService<String> testAuthorizationService() {
      return new TestRutebankenAuthorizationService();
    }
  }

  @Produce(
    "http:localhost:{{server.port}}/services/validation-report/" +
    TEST_CODESPACE +
    "/" +
    TEST_VALIDATION_REPORT_ID
  )
  protected ProducerTemplate validationReportTemplate;

  @Test
  void getValidationReport() throws Exception {
    // Prepare test data - create a mock validation report JSON
    String testReportJson =
      "{\"validationReportId\":\"" +
      TEST_VALIDATION_REPORT_ID +
      "\",\"codespace\":\"" +
      TEST_CODESPACE +
      "\"}";
    String fileStorePath =
      BLOBSTORE_PATH_ANTU_REPORTS +
      TEST_CODESPACE +
      VALIDATION_REPORT_PREFIX +
      TEST_VALIDATION_REPORT_ID +
      VALIDATION_REPORT_SUFFIX;

    InputStream testReportStream = new ByteArrayInputStream(
      testReportJson.getBytes(StandardCharsets.UTF_8)
    );

    // Populate fake blob repository
    antuInMemoryBlobStoreRepository.uploadBlob(fileStorePath, testReportStream);

    context.start();

    // Make REST call
    Map<String, Object> headers = getTestHeaders("GET");
    InputStream response =
      (InputStream) validationReportTemplate.requestBodyAndHeaders(
        null,
        headers
      );

    // Verify response
    assertNotNull(response);
    byte[] responseBytes = response.readAllBytes();
    assertTrue(responseBytes.length > 0);
    String responseContent = new String(responseBytes, StandardCharsets.UTF_8);
    assertTrue(responseContent.contains(TEST_VALIDATION_REPORT_ID));
  }

  private static Map<String, Object> getTestHeaders(String method) {
    return Map.of(
      Exchange.HTTP_METHOD,
      method,
      HttpHeaders.AUTHORIZATION,
      "Bearer test-token"
    );
  }
}
