package no.entur.antu.security.oauth2;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.entur.oauth2.multiissuer.MultiIssuerAuthenticationManagerResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = AntuWebSecurityConfigurationTest.MinimalConfig.class
)
@AutoConfigureWebMvc
@AutoConfigureMockMvc
class AntuWebSecurityConfigurationTest {

  @SpringBootConfiguration
  @EnableWebSecurity
  @Import(AntuWebSecurityConfiguration.class)
  @ImportAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
  static class MinimalConfig {}

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private MultiIssuerAuthenticationManagerResolver multiIssuerAuthenticationManagerResolver;

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @ParameterizedTest
  @ValueSource(
    strings = {
      "/services/validation-report/swagger.json",
      "/services/swagger.json",
      "/actuator/prometheus",
      "/actuator/health",
      "/actuator/health/liveness",
      "/actuator/health/readiness",
      "/actuator/info",
    }
  )
  void publicEndpointsDoNotRequireAuthentication(String path) throws Exception {
    mockMvc
      .perform(get(path))
      .andExpect(status().is(not(equalTo(HttpStatus.UNAUTHORIZED.value()))));
  }

  @Test
  void otherEndpointsRequireAuthentication() throws Exception {
    mockMvc
      .perform(get("/some-protected-endpoint"))
      .andExpect(status().isUnauthorized());
  }
}
