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

package no.entur.antu;

public final class Constants {

  /**
   * Header used internally in Antu to reference a file in the blob store.
   */
  public static final String FILE_HANDLE = "EnturFileHandle";
  public static final String NETEX_FILE_NAME = "EnturNetexFileName";
  public static final String NETEX_COMMON_FILE_NAME =
    "EnturNetexCommonFileName";

  public static final String TEMPORARY_FILE_NAME = "EnturTemporaryFileName";
  public static final String FILENAME_DELIMITER = "§";

  public static final String DATASET_REFERENTIAL = "EnturDatasetReferential";
  public static final String DATASET_CODESPACE = "EnturDatasetCodespace";

  public static final String DATASET_NB_NETEX_FILES =
    "EnturDatasetNbNetexFiles";
  public static final String DATASET_NB_COMMON_FILES =
    "EnturDatasetNbCommonFiles";

  public static final String DATASET_STATUS = "EnturDatasetStatus";

  public static final String JOB_TYPE = "JOB_TYPE";
  public static final String JOB_TYPE_SPLIT = "SPLIT";
  public static final String JOB_TYPE_VALIDATE = "VALIDATE";
  public static final String JOB_TYPE_VALIDATE_DATASET = "VALIDATE_DATASET";
  public static final String JOB_TYPE_COMPLETE_VALIDATION =
    "COMPLETE_VALIDATION";
  public static final String JOB_TYPE_AGGREGATE_REPORTS = "AGGREGATE_REPORTS";
  public static final String JOB_TYPE_AGGREGATE_COMMON_FILES =
    "AGGREGATE_COMMON_FILES";
  public static final String JOB_TYPE_REFRESH_STOP_CACHE =
    "REFRESH_STOP_PLACE_CACHE";
  public static final String JOB_TYPE_REFRESH_ORGANISATION_CACHE =
    "REFRESH_ORGANISATION_CACHE";
  public static final String JOB_TYPE_REFRESH_ORGANISATION_ALIAS_CACHE =
    "REFRESH_ORGANISATION_ALIAS_CACHE";

  public static final String STATUS_VALIDATION_STARTED = "started";
  public static final String STATUS_VALIDATION_OK = "ok";
  public static final String STATUS_VALIDATION_FAILED = "failed";

  public static final String VALIDATION_REPORT_ID_HEADER =
    "EnturValidationReportId";

  public static final String REPORT_CREATION_DATE = "reportCreationDate";

  public static final String BLOBSTORE_PATH_ANTU_EXCHANGE_INBOUND_RECEIVED =
    "inbound/received/";
  public static final String BLOBSTORE_PATH_ANTU_REPORTS = "reports/";
  public static final String BLOBSTORE_PATH_ANTU_WORK = "work/";
  public static final String GCS_BUCKET_FILE_NAME =
    BLOBSTORE_PATH_ANTU_WORK +
    "${header." +
    DATASET_REFERENTIAL +
    "}/${header." +
    VALIDATION_REPORT_ID_HEADER +
    "}/${header." +
    NETEX_FILE_NAME +
    "}.zip";

  /**
   * Headers originating from the validation client that must be sent back when notifying the validation client
   */
  public static final String VALIDATION_STAGE_HEADER = "EnturValidationStage";
  public static final String VALIDATION_IMPORT_TYPE =
    "EnturValidationImportType";
  public static final String VALIDATION_CLIENT_HEADER = "EnturValidationClient";
  public static final String VALIDATION_PROFILE_HEADER =
    "EnturValidationProfile";
  public static final String VALIDATION_DATASET_FILE_HANDLE_HEADER =
    "EnturValidationDatasetFileHandle";
  public static final String VALIDATION_CORRELATION_ID_HEADER =
    "EnturValidationCorrelationId";

  public static final String VALIDATION_CLIENT_MARDUK = "Marduk";
  public static final String VALIDATION_CLIENT_KAKKA = "Kakka";

  public static final String PROP_NETEX_FILE_CONTENT = "NETEX_FILE_CONTENT";

  public static final String CAMEL_ALL_HTTP_HEADERS = "CamelHttp*";
  public static final String VALIDATION_REPORT_PREFIX = "/validation-report-";
  public static final String VALIDATION_REPORT_SUFFIX = ".json";
  public static final String VALIDATION_REPORT_STATUS_SUFFIX = ".status";

  public static final String NSR_XMLNS = "NSR";
  public static final String NSR_XMLNSURL = "http://www.rutebanken.org/ns/nsr";

  public static final String PEN_XMLNS = "PEN";
  public static final String PEN_XMLNSURL = "http://www.rutebanken.org/ns/pen";

  /**
   * Constants used towards APIs
   */
  public static final String ORGANISATION_ET_CLIENT_NAME_HEADER =
    "Et-Client-Name";
  public static final String ORGANISATION_ET_CLIENT_NAME_HEADER_VALUE =
    "entur-antu";

  private Constants() {}
}
