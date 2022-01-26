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
    public static final String FILE_HANDLE = "RutebankenFileHandle";
    public static final String NETEX_FILE_NAME = "NETEX_FILE_NAME";
    public static final String FILENAME_DELIMITER = "§";

    public static final String DATASET_REFERENTIAL = "EnturDatasetReferential";
    public static final String DATASET_CODESPACE = "EnturDatasetCodespace";

    public static final String DATASET_NB_NETEX_FILES = "EnturDatasetNbNetexFiles";
    public static final String DATASET_NB_COMMON_FILES = "EnturDatasetNbCommonFiles";

    public static final String DATASET_STATUS = "EnturDatasetStatus";

    public static final String JOB_TYPE = "JOB_TYPE";
    public static final String JOB_TYPE_SPLIT = "SPLIT";
    public static final String JOB_TYPE_VALIDATE = "VALIDATE";
    public static final String JOB_TYPE_AGGREGATE_REPORTS = "AGGREGATE_REPORTS";
    public static final String JOB_TYPE_AGGREGATE_COMMON_FILES = "AGGREGATE_COMMON_FILES";

    public static final String STATUS_VALIDATION_STARTED = "started";
    public static final String STATUS_VALIDATION_OK = "ok";
    public static final String STATUS_VALIDATION_FAILED = "failed";

    public static final String VALIDATION_REPORT_ID = "EnturValidationReportId";

    public static final String BLOBSTORE_PATH_MARDUK_INBOUND_RECEIVED = "inbound/received/";
    public static final String BLOBSTORE_PATH_ANTU_REPORTS = "reports/";
    public static final String BLOBSTORE_PATH_ANTU_WORK = "work/";
    public static final String GCS_BUCKET_FILE_NAME = BLOBSTORE_PATH_ANTU_WORK + "${header." + DATASET_REFERENTIAL + "}/${header." + VALIDATION_REPORT_ID + "}/${header." + NETEX_FILE_NAME + "}.zip";

    /**
     * Headers originating from the validation client that must be sent back when notifying the validation client
     */
    public static final String VALIDATION_STAGE_HEADER = "EnturValidationStage";
    public static final String VALIDATION_CLIENT_HEADER = "EnturValidationClient";
    public static final String CORRELATION_ID = "RutebankenCorrelationId";

    public static final String CAMEL_ALL_HTTP_HEADERS = "CamelHttp*";
    public static final String VALIDATION_REPORT_PREFIX = "/validation-report-";

    public static final String AGGREGATED_VALIDATION_REPORT = "AGGREGATED_VALIDATION_REPORT";

    public static final String NSR_XMLNS = "NSR";
    public static final String NSR_XMLNSURL = "http://www.rutebanken.org/ns/nsr";

    public static final String PEN_XMLNS = "PEN";
    public static final String PEN_XMLNSURL = "http://www.rutebanken.org/ns/pen";


    private Constants() {
    }

}

