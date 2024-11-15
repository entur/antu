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
import static no.entur.antu.util.NetexFileUtils.buildFileNamesListFromSet;

import java.util.Set;
import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.util.NetexFileUtils;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

/**
 * Create a validation job for each common file.
 * Validation jobs for line files are created subsequently after all common files have been validated (see {@link CommonFilesBarrierRouteBuilder})
 * If the dataset does not contain any common file, the common file barrier is bypassed and validation jobs for line
 * files are created.
 */
@Component
public class CommonFileValidationRouteBuilder extends BaseRouteBuilder {

  private static final String PROP_STOP_WATCH = "PROP_STOP_WATCH";

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:validateCommonFiles")
      .process(exchange -> {
        long nbCommonFiles =
          (
            (Set<String>) exchange.getProperty(
              PROP_DATASET_NETEX_FILE_NAMES_SET,
              Set.class
            )
          ).stream()
            .filter(NetexFileUtils::isCommonFile)
            .count();
        exchange.getIn().setHeader(DATASET_NB_COMMON_FILES, nbCommonFiles);
      })
      .choice()
      .when(header(DATASET_NB_COMMON_FILES).isGreaterThan(0))
      .to("direct:createCommonFilesValidationJobs")
      .otherwise()
      // skip the common file barrier and go directly to the line file job creation step
      .process(exchange ->
        exchange
          .getIn()
          .setBody(
            buildFileNamesListFromSet(
              exchange.getProperty(PROP_DATASET_NETEX_FILE_NAMES_SET, Set.class)
            )
          )
      )
      .log(LoggingLevel.TRACE, correlation() + "All NeTEx Files: ${body}")
      .to("direct:createLineFilesValidationJobs")
      // end otherwise
      .end()
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Split NeTEx file in ${exchangeProperty." +
        PROP_STOP_WATCH +
        ".taken()} ms"
      );

    from("direct:createCommonFilesValidationJobs")
      .log(
        LoggingLevel.INFO,
        correlation() + "Creating common files validation jobs"
      )
      .split(exchangeProperty(PROP_DATASET_NETEX_FILE_NAMES_SET))
      .filter(body().startsWith("_"))
      .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_VALIDATE))
      .setHeader(
        Constants.DATASET_NB_NETEX_FILES,
        exchangeProperty(Exchange.SPLIT_SIZE)
      )
      .setHeader(NETEX_FILE_NAME, body())
      .setHeader(FILE_HANDLE, simple(Constants.GCS_BUCKET_FILE_NAME))
      .process(exchange ->
        exchange
          .getIn()
          .setBody(
            buildFileNamesListFromSet(
              exchange.getProperty(PROP_DATASET_NETEX_FILE_NAMES_SET, Set.class)
            )
          )
      )
      .log(LoggingLevel.TRACE, correlation() + "All NeTEx Files: ${body}")
      .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
      //end split
      .end()
      .routeId("create-common-files-validation-jobs");
  }
}
