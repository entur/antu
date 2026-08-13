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

  public static final String NETEX_FILE_NAME = "EnturNetexFileName";

  /**
   * Separates the NeTEx file names carried in a job message body.
   */
  public static final String FILENAME_DELIMITER = "§";

  public static final String DATASET_REFERENTIAL = "EnturDatasetReferential";
  public static final String DATASET_CODESPACE = "EnturDatasetCodespace";

  public static final String DATASET_NB_NETEX_FILES =
    "EnturDatasetNbNetexFiles";
  public static final String DATASET_NB_COMMON_FILES =
    "EnturDatasetNbCommonFiles";

  public static final String JOB_TYPE = "JOB_TYPE";

  public static final String STATUS_VALIDATION_STARTED = "started";
  public static final String STATUS_VALIDATION_OK = "ok";
  public static final String STATUS_VALIDATION_FAILED = "failed";
  public static final String STATUS_VALIDATION_TIMEOUT = "timeout";

  public static final String VALIDATION_REPORT_ID_HEADER =
    "EnturValidationReportId";

  public static final String BLOBSTORE_PATH_ANTU_EXCHANGE_INBOUND_RECEIVED =
    "inbound/received/";
  public static final String BLOBSTORE_PATH_ANTU_REPORTS = "reports/";
  public static final String BLOBSTORE_PATH_ANTU_WORK = "work/";

  /**
   * PubSub attributes originating from the validation client that must be sent back when notifying it.
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

  public static final String FILE_CREATED_TIMESTAMP_HEADER =
    "FileCreatedTimestamp";
  public static final String RUTEBANKEN_FILE_HANDLE_HEADER =
    "RutebankenFileHandle";

  public static final String VALIDATION_CLIENT_MARDUK = "Marduk";
  public static final String VALIDATION_CLIENT_KAKKA = "Kakka";

  public static final String VALIDATION_REPORT_PREFIX = "/validation-report-";
  public static final String VALIDATION_REPORT_SUFFIX = ".json";
  public static final String VALIDATION_REPORT_STATUS_SUFFIX = ".status";

  public static final String NSR_XMLNS = "NSR";
  public static final String NSR_XMLNSURL = "http://www.rutebanken.org/ns/nsr";

  /**
   * Constants used towards APIs
   */
  public static final String ET_CLIENT_NAME_HEADER = "Et-Client-Name";
  public static final String ET_CLIENT_NAME_HEADER_VALUE = "entur-antu";

  private Constants() {}
}
