package no.entur.antu.rest;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Readiness and liveness endpoint for the Kubernetes probes.
 *
 * <p>Answers from a constant so that it reports on the HTTP stack alone and never fails because Redis,
 * GCS or PubSub are having a bad day: a pod that cannot reach them should keep serving reports, not
 * drop out of the Service.
 */
@RestController
public class HealthController {

  @GetMapping(path = "/services/health", produces = MediaType.TEXT_PLAIN_VALUE)
  public String health() {
    return "OK";
  }
}
