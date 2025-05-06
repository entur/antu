/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.antu.routes.validation;

import static no.entur.antu.Constants.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

/**
 * Process NeTEX validation requests.
 */
@Component
public class InitValidationRouteBuilder extends BaseRouteBuilder {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
    DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");
  private static final String PROP_REPORT_ID_TIMESTAMP = "REPORT_ID_TIMESTAMP";

  @Override
  public void configure() throws Exception {
    super.configure();

    from("google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationQueue")
      .to("direct:initDatasetValidation")
      .routeId("netex-validation-queue-pubsub");

    from("direct:initDatasetValidation")
      .process(this::setCorrelationIdIfMissing)
      .setHeader(
        DATASET_CODESPACE,
        header(DATASET_REFERENTIAL).regexReplaceAll("rb_", "")
      )
      .setProperty(
        PROP_REPORT_ID_TIMESTAMP,
        () -> DATE_TIME_FORMATTER.format(LocalDateTime.now())
      )
      .setHeader(
        Constants.VALIDATION_REPORT_ID_HEADER,
        header(DATASET_REFERENTIAL)
          .append('_')
          .append(exchangeProperty(PROP_REPORT_ID_TIMESTAMP))
      )
      .setBody(constant(STATUS_VALIDATION_STARTED))
      .to("direct:notifyStatus")
      .log(LoggingLevel.INFO, correlation() + "Starting validation")
      .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_SPLIT))
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
      .routeId("init-dataset-validation");

    from(
      "google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue?synchronousPull=true&concurrentConsumers={{antu.netex.job.consumers:1}}"
    )
      .to("direct:processJob")
      .routeId("process-job-queue-pubsub");

    from("direct:processJob")
      .choice()
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_SPLIT))
      .to("direct:splitDataset")
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_VALIDATE))
      .to("direct:validateNetex")
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_VALIDATE_DATASET))
      .to("direct:validateDataset")
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_COMPLETE_VALIDATION))
      .to("direct:completeValidation")
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_AGGREGATE_COMMON_FILES))
      .to("direct:createLineFilesValidationJobs")
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_AGGREGATE_REPORTS))
      .to("direct:aggregateReports")
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_REFRESH_STOP_CACHE))
      .to("direct:refreshStopCache")
      .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_REFRESH_ORGANISATION_CACHE))
      .to("direct:refreshOrganisationCache")
      .when(
        header(JOB_TYPE).isEqualTo(JOB_TYPE_REFRESH_ORGANISATION_ALIAS_CACHE)
      )
      .to("direct:refreshOrganisationAliasCache")
      .otherwise()
      .log(
        LoggingLevel.ERROR,
        correlation() +
        "Unknown job type ${header." +
        Constants.JOB_TYPE +
        " } "
      )
      .routeId("process-job");

    from("direct:notifyStatus")
      .log(LoggingLevel.INFO, correlation() + "Notifying status ${body}")
      .to(
        "google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationStatusQueue"
      )
      .routeId("notify-status");
  }
}
