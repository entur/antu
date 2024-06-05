package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.DATASET_STATUS;
import static no.entur.antu.Constants.STATUS_VALIDATION_FAILED;
import static no.entur.antu.Constants.STATUS_VALIDATION_OK;

import no.entur.antu.metrics.AntuPrometheusMetricsService;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;

public class CompleteValidationRouteBuilder extends BaseRouteBuilder {

  private final AntuPrometheusMetricsService antuPrometheusMetricsService;

  public CompleteValidationRouteBuilder(
    AntuPrometheusMetricsService antuPrometheusMetricsService
  ) {
    this.antuPrometheusMetricsService = antuPrometheusMetricsService;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:completeValidation2")
      .choice()
      .when(simple("${body.hasError()}"))
      .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_FAILED))
      .log(LoggingLevel.INFO, correlation() + "Validation errors found")
      .otherwise()
      .setHeader(DATASET_STATUS, constant(STATUS_VALIDATION_OK))
      .log(LoggingLevel.INFO, correlation() + "No validation error")
      .to("direct:uploadValidationReportMetrics")
      .end()
      .marshal()
      .json(JsonLibrary.Jackson)
      .to("direct:uploadAggregatedValidationReport")
      .setBody(header(DATASET_STATUS))
      .to("direct:notifyStatus")
      .to("direct:createValidationReportStatusFile")
      .to("direct:cleanUpCache")
      .routeId("complete-validation");

    from("direct:uploadValidationReportMetrics")
      .bean(antuPrometheusMetricsService)
      .routeId("upload-validation-report-metrics");
  }
}
