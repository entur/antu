package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.VALIDATION_PROFILE_HEADER;

import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.validation.AntuNetexValidationProgressCallback;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

@Component
public class DatasetValidationRouteBuilder extends BaseRouteBuilder {

  private static final String PROP_NETEX_VALIDATION_CALLBACK =
    "PROP_NETEX_VALIDATION_CALLBACK";

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:validateDataset2")
      .process(exchange ->
        exchange.setProperty(
          PROP_NETEX_VALIDATION_CALLBACK,
          new AntuNetexValidationProgressCallback(this, exchange)
        )
      )
      .bean(
        "netexValidationProfile",
        "validateDataset(" +
        "${body}, " +
        "${header." +
        VALIDATION_PROFILE_HEADER +
        "},${exchangeProperty." +
        PROP_NETEX_VALIDATION_CALLBACK +
        "})"
      )
      .log(
        LoggingLevel.DEBUG,
        correlation() + "Completed all NeTEx dataset validators"
      )
      .routeId("validate-dataset2");
  }
}
