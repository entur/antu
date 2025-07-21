package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.*;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import no.entur.antu.AntuRouteBuilderIntegrationTestBase;
import no.entur.antu.TestApp;
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class,
  properties = { "antu.netex.validation.entries.max=1" }
)
class ValidateFilesRouteBuilderTest
  extends AntuRouteBuilderIntegrationTestBase {

  @Produce("direct:validateNetex")
  private ProducerTemplate validateNetex;

  @EndpointInject("mock:downloadSingleNetexFileFromMemoryStore")
  private MockEndpoint downloadSingleNetexFileFromMemoryStore;

  @EndpointInject("mock:runNetexValidators")
  private MockEndpoint runNetexValidators;

  @EndpointInject("mock:reportSystemError")
  private MockEndpoint reportSystemError;

  @EndpointInject("mock:notifyValidationReportAggregator")
  private MockEndpoint notifyValidationReportAggregator;

  private Map<String, Object> headers;

  @BeforeEach
  void setUp() throws Exception {
    headers = new HashMap<>();
    headers.put(DATASET_CODESPACE, "codespace");
    headers.put(VALIDATION_REPORT_ID_HEADER, "reportId");
    headers.put(FILE_HANDLE, "netex.xml");
    headers.put(NETEX_FILE_NAME, "netex.xml");

    AdviceWith.adviceWith(
      context,
      "validate-netex",
      a -> {
        a
          .interceptSendToEndpoint("direct:extendAckDeadline")
          .skipSendToOriginalEndpoint()
          .to("mock:sink");

        a
          .interceptSendToEndpoint(
            "direct:downloadSingleNetexFileFromMemoryStore"
          )
          .skipSendToOriginalEndpoint()
          .to("mock:downloadSingleNetexFileFromMemoryStore");

        a
          .interceptSendToEndpoint("direct:runNetexValidators")
          .skipSendToOriginalEndpoint()
          .to("mock:runNetexValidators");

        a
          .interceptSendToEndpoint("direct:reportSystemError")
          .skipSendToOriginalEndpoint()
          .to("mock:reportSystemError");

        a
          .interceptSendToEndpoint("direct:notifyValidationReportAggregator")
          .skipSendToOriginalEndpoint()
          .to("mock:notifyValidationReportAggregator");
      }
    );
  }

  @Test
  void validateMissingFile() throws InterruptedException {
    downloadSingleNetexFileFromMemoryStore.whenAnyExchangeReceived(exchange -> {
      throw new AntuMemoryStoreFileNotFoundException("file not found");
    });
    reportSystemError.expectedMessageCount(0);
    notifyValidationReportAggregator.expectedMessageCount(0);
    context.start();
    validateNetex.sendBodyAndHeaders(" ", headers);
    reportSystemError.assertIsSatisfied();
    notifyValidationReportAggregator.assertIsSatisfied();
  }

  @Test
  void validateInterrupted() throws InterruptedException {
    runNetexValidators.whenAnyExchangeReceived(exchange -> {
      throw new InterruptedException("interrupted");
    });
    reportSystemError.expectedMessageCount(0);
    notifyValidationReportAggregator.expectedMessageCount(0);
    context.start();
    assertThrows(
      CamelExecutionException.class,
      () -> validateNetex.sendBodyAndHeaders(" ", headers),
      "Retryable exceptions should be rethrown so that the PubSub message is not acked"
    );
    reportSystemError.assertIsSatisfied();
    notifyValidationReportAggregator.assertIsSatisfied();
  }

  @Test
  void validateOtherException() throws InterruptedException {
    runNetexValidators.whenAnyExchangeReceived(exchange -> {
      throw new RuntimeException("other exception");
    });

    reportSystemError.expectedMessageCount(1);
    reportSystemError.whenAnyExchangeReceived(exchange -> {
      exchange
        .getIn()
        .setBody(
          new ValidationReport(
            exchange.getIn().getHeader(DATASET_CODESPACE, String.class),
            exchange
              .getIn()
              .getHeader(VALIDATION_REPORT_ID_HEADER, String.class)
          )
        );
    });
    notifyValidationReportAggregator.expectedMessageCount(1);
    context.start();
    validateNetex.sendBodyAndHeaders(" ", headers);

    reportSystemError.assertIsSatisfied();
    notifyValidationReportAggregator.assertIsSatisfied();
  }
}
