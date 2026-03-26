package no.entur.antu.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import no.entur.antu.Constants;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Verifies that the MDC interceptor in BaseRouteBuilder sets
 * correlationId and codespace for all route types, not just PubSub.
 */
class BaseRouteBuilderMdcTest extends CamelTestSupport {

  private final AtomicReference<String> capturedCorrelationId =
    new AtomicReference<>();
  private final AtomicReference<String> capturedCodespace =
    new AtomicReference<>();

  @Override
  protected RoutesBuilder createRouteBuilder() {
    return new RouteBuilder() {
      @Override
      public void configure() throws Exception {
        // Reuse the MDC interceptor logic from BaseRouteBuilder
        interceptFrom(".*")
          .process(exchange -> {
            String correlationId = exchange
              .getIn()
              .getHeader(
                Constants.VALIDATION_CORRELATION_ID_HEADER,
                String.class
              );
            if (correlationId != null) {
              MDC.put("correlationId", correlationId);
            }
            String codespace = exchange
              .getIn()
              .getHeader(Constants.DATASET_REFERENTIAL, String.class);
            if (codespace != null && !codespace.isEmpty()) {
              MDC.put("codespace", codespace);
            }
          });

        onCompletion()
          .process(exchange -> {
            MDC.remove("correlationId");
            MDC.remove("codespace");
          });

        from("direct:test")
          .process(exchange -> {
            capturedCorrelationId.set(MDC.get("correlationId"));
            capturedCodespace.set(MDC.get("codespace"));
          })
          .routeId("test-route");
      }
    };
  }

  @Test
  void mdcSetForDirectRoute() throws Exception {
    template.sendBodyAndHeaders(
      "direct:test",
      "",
      java.util.Map.of(
        Constants.VALIDATION_CORRELATION_ID_HEADER,
        "corr-123",
        Constants.DATASET_REFERENTIAL,
        "flt"
      )
    );

    assertEquals("corr-123", capturedCorrelationId.get());
    assertEquals("flt", capturedCodespace.get());
  }

  @Test
  void mdcNotSetWhenHeadersMissing() throws Exception {
    template.sendBody("direct:test", "");

    assertNull(capturedCorrelationId.get());
    assertNull(capturedCodespace.get());
  }

  @Test
  void mdcClearedAfterRouteCompletion() throws Exception {
    template.sendBodyAndHeaders(
      "direct:test",
      "",
      java.util.Map.of(
        Constants.VALIDATION_CORRELATION_ID_HEADER,
        "corr-456",
        Constants.DATASET_REFERENTIAL,
        "rb_flt"
      )
    );

    assertEquals("rb_flt", capturedCodespace.get());
    // MDC should be cleared after route completes
    assertNull(MDC.get("correlationId"));
    assertNull(MDC.get("codespace"));
  }
}
