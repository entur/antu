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

import java.util.HashSet;
import java.util.Set;
import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.util.NetexFileUtils;
import no.entur.antu.validation.NetexValidationWorkflow;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.LoggingLevel;
import org.apache.camel.dataformat.zipfile.ZipSplitter;
import org.apache.camel.util.StopWatch;
import org.springframework.stereotype.Component;

/**
 * Extract NeTEx files from a NeTEx archive, save them individually in a bucket.
 * If XML Schema validation should be run, then create XML Schema validation jobs,
 * else if there are common files in the dataset,  then create common files validation jobs,
 * otherwise create line file validation jobs.
 * Validation jobs for line files are created subsequently after all common files have been validated (see {@link CommonFilesBarrierRouteBuilder})
 *
 */
@Component
public class SplitDatasetRouteBuilder extends BaseRouteBuilder {

  private final NetexValidationWorkflow netexValidationWorkflow;

  public SplitDatasetRouteBuilder(
    NetexValidationWorkflow netexValidationWorkflow
  ) {
    this.netexValidationWorkflow = netexValidationWorkflow;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:splitDataset")
      .streamCaching()
      .setProperty(PROP_STOP_WATCH, StopWatch::new)
      .setHeader(FILE_HANDLE, header(VALIDATION_DATASET_FILE_HANDLE_HEADER))
      .to("direct:downloadNetexDataset")
      .log(LoggingLevel.INFO, correlation() + "Uploading NeTEx files")
      .to("direct:uploadSingleNetexFiles")
      .log(LoggingLevel.INFO, correlation() + "Uploaded NeTEx files")
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
      .process(e -> {
        boolean hasSchemaValidator = netexValidationWorkflow.hasSchemaValidator(
          e.getIn().getHeader(VALIDATION_PROFILE_HEADER, String.class)
        );
        e.getIn().setBody(hasSchemaValidator);
      })
      .choice()
      .when(simple("${body} == true"))
      .to("direct:createXMLSchemaValidationJobs")
      .when(header(DATASET_NB_COMMON_FILES).isGreaterThan(0))
      .to("direct:createCommonFilesValidationJobs")
      .otherwise()
      // skip the XML schema validation barrier and the common files barrier
      // and go directly to the line file job creation step
      .process(e ->
        e
          .getIn()
          .setBody(
            NetexFileUtils.buildFileNamesListFromSet(
              e.getProperty(PROP_DATASET_NETEX_FILE_NAMES_SET, Set.class)
            )
          )
      )
      .log(LoggingLevel.TRACE, correlation() + "All NeTEx Files: ${body}")
      .to("direct:createLineFilesValidationJobs")
      .end()
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Split NeTEx files in ${exchangeProperty." +
        PROP_STOP_WATCH +
        ".taken()} ms"
      )
      .routeId("split-dataset");

    from("direct:downloadNetexDataset")
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Downloading NeTEx dataset ${header." +
        FILE_HANDLE +
        "}"
      )
      .to("direct:getAntuExchangeBlob")
      .filter(body().isNull())
      .log(
        LoggingLevel.ERROR,
        correlation() + "NeTEx file not found: ${header." + FILE_HANDLE + "}"
      )
      .stop()
      //end filter
      .end()
      .routeId("download-netex-dataset");

    from("direct:uploadSingleNetexFiles")
      .split(new ZipSplitter())
      .aggregationStrategy(new SingleNetexFileAggregationStrategy())
      .streaming()
      .filter()
      .method(NetexFileCounter.class, "everyHundredFiles")
      .log(
        LoggingLevel.INFO,
        correlation() + "Uploaded ${header.CamelSplitIndex} NeTEx files"
      )
      .process(this::extendAckDeadline)
      .end()
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Processing NeTEx file ${header." +
        Exchange.FILE_NAME +
        "}"
      )
      .choice()
      .when(header(Exchange.FILE_NAME).endsWith(".xml"))
      .to("direct:uploadSingleNetexFile")
      .otherwise()
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Ignoring non-XML file ${header." +
        Exchange.FILE_NAME +
        "}"
      )
      .setBody(constant(""))
      .routeId("upload-single-netex-files");

    from("direct:uploadSingleNetexFile")
      .setHeader(NETEX_FILE_NAME, header(Exchange.FILE_NAME))
      .marshal()
      .zipFile()
      .setHeader(FILE_HANDLE, simple(Constants.GCS_BUCKET_FILE_NAME))
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Uploading NeTEx file ${header." +
        NETEX_FILE_NAME +
        "} to GCS file ${header." +
        FILE_HANDLE +
        "}"
      )
      .setHeader(TEMPORARY_FILE_NAME, header(NETEX_FILE_NAME))
      .to("direct:uploadBlobToMemoryStore")
      .routeId("upload-single-netex-file");
  }

  public static final class SingleNetexFileAggregationStrategy
    implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
      if (oldExchange == null) {
        Set<String> netexFileNames = new HashSet<>();
        newExchange.setProperty(
          PROP_DATASET_NETEX_FILE_NAMES_SET,
          netexFileNames
        );
        String currentFileName = newExchange
          .getIn()
          .getHeader(Exchange.FILE_NAME, String.class);
        if (currentFileName.endsWith(".xml.zip")) {
          netexFileNames.add(removeZipExtension(currentFileName));
        }
        return newExchange;
      }
      String currentFileName = newExchange
        .getIn()
        .getHeader(Exchange.FILE_NAME, String.class);
      if (currentFileName.endsWith(".xml.zip")) {
        Set<String> netexFileNames = oldExchange.getProperty(
          PROP_DATASET_NETEX_FILE_NAMES_SET,
          Set.class
        );
        netexFileNames.add(removeZipExtension(currentFileName));
      }

      return oldExchange;
    }

    private String removeZipExtension(String currentFileName) {
      return currentFileName.substring(0, currentFileName.length() - 4);
    }
  }

  private static class NetexFileCounter {

    public static boolean everyHundredFiles(
      @Header("CamelSplitIndex") int index
    ) {
      return index % 100 == 0;
    }
  }
}
