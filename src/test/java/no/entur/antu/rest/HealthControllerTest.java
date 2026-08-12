package no.entur.antu.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * The Kubernetes probes target this path, so it is pinned here.
 */
class HealthControllerTest {

  @Test
  void healthAnswersOk() throws Exception {
    MockMvcBuilders
      .standaloneSetup(new HealthController())
      .build()
      .perform(get("/services/health"))
      .andExpect(status().isOk())
      .andExpect(content().string("OK"));
  }
}
