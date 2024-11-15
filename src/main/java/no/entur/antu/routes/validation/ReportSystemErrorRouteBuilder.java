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

import no.entur.antu.routes.BaseRouteBuilder;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.springframework.stereotype.Component;

/**
 * Report System Error.
 */
@Component
public class ReportSystemErrorRouteBuilder extends BaseRouteBuilder {

  @Override
  public void configure() throws Exception {
    super.configure();

    from("direct:reportSystemError")
      .process(exchange -> {
        String codespace = exchange
          .getIn()
          .getHeader(DATASET_CODESPACE, String.class);
        String validationReportId = exchange
          .getIn()
          .getHeader(VALIDATION_REPORT_ID_HEADER, String.class);
        ValidationReport validationReport = new ValidationReport(
          codespace,
          validationReportId
        );
        String fileName = exchange
          .getIn()
          .getHeader(NETEX_FILE_NAME, String.class);
        ValidationReportEntry validationReportEntry = new ValidationReportEntry(
          "System error while validating the file " +
          exchange.getIn().getHeader(NETEX_FILE_NAME),
          "SYSTEM_ERROR",
          ValidationReportEntrySeverity.ERROR,
          new DataLocation(null, fileName, null, null)
        );
        validationReport.addValidationReportEntry(validationReportEntry);
        exchange.getIn().setBody(validationReport, ValidationReport.class);
      })
      .routeId("report-system-error");
  }
}
