/*
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.antu.routes.memorystore;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_WORK;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.TEMPORARY_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;

import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

/**
 * Upload and download files to a memory store.
 * This is intended to be used for temporary files created during the validation process and shared between Kubernetes prods.
 * Using a memory store gives better performance than storing temporary files in Google Cloud Storage.
 */
@Component
public class MemoryStoreRouteBuilder extends BaseRouteBuilder {

  public static final String MEMORY_STORE_FILE_NAME = "MemoryStoreFileName";

  @Override
  public void configure() {
    from("direct:saveValidationReport")
      .log(LoggingLevel.DEBUG, correlation() + "Saving validation report")
      .marshal()
      .json(JsonLibrary.Jackson)
      .to("direct:uploadValidationReport")
      .log(LoggingLevel.DEBUG, correlation() + "Saved validation report")
      .routeId("save-validation-report");

    from("direct:uploadValidationReport")
      .setHeader(
        TEMPORARY_FILE_NAME,
        constant(BLOBSTORE_PATH_ANTU_WORK)
          .append(header(DATASET_REFERENTIAL))
          .append("/")
          .append(header(VALIDATION_REPORT_ID_HEADER))
          .append("/")
          .append(header(NETEX_FILE_NAME))
          .append(VALIDATION_REPORT_SUFFIX)
      )
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Uploading Validation Report  to GCS file ${header." +
        TEMPORARY_FILE_NAME +
        "}"
      )
      .to("direct:uploadBlobToMemoryStore")
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Uploaded Validation Report to GCS file ${header." +
        TEMPORARY_FILE_NAME +
        "}"
      )
      .routeId("upload-validation-report");

    from("direct:downloadBlobFromMemoryStore")
      .bean(
        "temporaryFileRepository",
        "download(${header." +
        VALIDATION_REPORT_ID_HEADER +
        "},${header." +
        TEMPORARY_FILE_NAME +
        "})"
      )
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Returning from fetching file ${header." +
        TEMPORARY_FILE_NAME +
        "} from memory store."
      )
      .routeId("memory-store-download");

    from("direct:uploadBlobToMemoryStore")
      .bean(
        "temporaryFileRepository",
        "upload(${header." +
        VALIDATION_REPORT_ID_HEADER +
        "},${header." +
        TEMPORARY_FILE_NAME +
        "}, ${body} )"
      )
      .setBody(constant(""))
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Stored file ${header." +
        TEMPORARY_FILE_NAME +
        "} in memory store."
      )
      .routeId("memory-store-upload");

    from("direct:downloadSingleNetexFileFromMemoryStore")
      .streamCache("true")
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Downloading single NeTEx file ${header." +
        FILE_HANDLE +
        "}"
      )
      .setHeader(TEMPORARY_FILE_NAME, header(MEMORY_STORE_FILE_NAME))
      .to("direct:downloadBlobFromMemoryStore")
      .log(
        LoggingLevel.DEBUG,
        correlation() +
        "Downloaded single NeTEx file ${header." +
        FILE_HANDLE +
        "}"
      )
      .unmarshal()
      .zipFile()
      .routeId("memory-store-download-single-netex-file");
  }
}
