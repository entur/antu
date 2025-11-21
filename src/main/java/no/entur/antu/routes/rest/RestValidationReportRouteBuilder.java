/*
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

package no.entur.antu.routes.rest;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_REPORTS;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.security.AntuAuthorizationService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.apache.hc.core5.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class RestValidationReportRouteBuilder extends BaseRouteBuilder {

  private static final String PLAIN = "text/plain";
  private static final String JSON = "application/json";
  private static final String SWAGGER_DATA_TYPE_STRING = "string";
  private static final String CODESPACE_PARAM = "codespace";
  private static final String VALIDATION_REPORT_ID_PARAM = "id";
  private static final String PATTERN_PARAM = "PATTERN";

  private final AntuAuthorizationService antuAuthorizationService;
  private final String host;
  private final String port;

  public RestValidationReportRouteBuilder(
    AntuAuthorizationService antuAuthorizationService,
    @Value("${server.host:0.0.0.0}") String host,
    @Value("${server.port:8080}") String port
  ) {
    this.antuAuthorizationService = antuAuthorizationService;
    this.host = host;
    this.port = port;
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    onException(AccessDeniedException.class)
      .handled(true)
      .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(403))
      .setHeader(Exchange.CONTENT_TYPE, constant(PLAIN))
      .transform(exceptionMessage());

    onException(BadRequestException.class)
      .handled(true)
      .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
      .setHeader(Exchange.CONTENT_TYPE, constant(PLAIN))
      .transform(exceptionMessage());

    onException(NotFoundException.class)
      .handled(true)
      .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
      .setHeader(Exchange.CONTENT_TYPE, constant(PLAIN))
      .transform(exceptionMessage());

    restConfiguration()
      .component("platform-http")
      .contextPath("/services")
      .bindingMode(RestBindingMode.off)
      .apiContextPath("/swagger.json")
      .apiProperty("api.title", "Antu NeTEx Validation API")
      .apiProperty("api.version", "1.0");

    rest("")
      .apiDocs(false)
      .description(
        "Wildcard definitions necessary to get Jetty to match authorization filters to endpoints with path params"
      )
      .get()
      .to("direct:adminRouteAuthorizeGet")
      .post()
      .to("direct:adminRouteAuthorizePost")
      .put()
      .to("direct:adminRouteAuthorizePut")
      .delete()
      .to("direct:adminRouteAuthorizeDelete");

    String commonApiDocEndpoint =
      "http:" +
      host +
      ":" +
      port +
      "/services/swagger.json?bridgeEndpoint=true";

    rest("/validation-report")
      .get("/{" + CODESPACE_PARAM + "}/{" + VALIDATION_REPORT_ID_PARAM + '}')
      .description(
        "Return the validation report for a given validation report id"
      )
      .param()
      .name(CODESPACE_PARAM)
      .type(RestParamType.path)
      .description("Provider Codespace")
      .dataType(SWAGGER_DATA_TYPE_STRING)
      .required(true)
      .endParam()
      .param()
      .name(VALIDATION_REPORT_ID_PARAM)
      .type(RestParamType.path)
      .description("Unique Id of the validation report")
      .dataType(SWAGGER_DATA_TYPE_STRING)
      .required(true)
      .endParam()
      .consumes(PLAIN)
      .produces(JSON)
      .responseMessage()
      .code(200)
      .endResponseMessage()
      .responseMessage()
      .code(404)
      .message("Unknown codespace")
      .endResponseMessage()
      .responseMessage()
      .code(500)
      .message("Internal error")
      .endResponseMessage()
      .to("direct:validationReport")
      .get("/swagger.json")
      .apiDocs(false)
      .bindingMode(RestBindingMode.off)
      .to(commonApiDocEndpoint);

    rest("/cache-admin")
      .post("/clear-cache")
      .description("Clear the cache")
      .consumes(PLAIN)
      .produces(PLAIN)
      .responseMessage()
      .code(200)
      .message("Command accepted")
      .endResponseMessage()
      .to("direct:adminCacheClear")
      .post("/delete-by-pattern/{" + PATTERN_PARAM + "}")
      .description("Delete keys by pattern")
      .param()
      .name(PATTERN_PARAM)
      .type(RestParamType.path)
      .description("Key pattern")
      .dataType(SWAGGER_DATA_TYPE_STRING)
      .required(true)
      .endParam()
      .consumes(PLAIN)
      .produces(PLAIN)
      .responseMessage()
      .code(200)
      .message("Command accepted")
      .endResponseMessage()
      .to("direct:adminCacheDeleteByPattern")
      .get("/dump-keys")
      .description("Clear the cache")
      .consumes(PLAIN)
      .produces(PLAIN)
      .responseMessage()
      .code(200)
      .message("Command accepted")
      .endResponseMessage()
      .to("direct:adminCacheDumpKeys")
      .post("/refresh-stop-cache")
      .description("Refresh the stop cache")
      .consumes(PLAIN)
      .produces(PLAIN)
      .responseMessage()
      .code(200)
      .message("Command accepted")
      .endResponseMessage()
      .to("direct:adminRefreshStopCache")
      .post("/refresh-organisation-cache")
      .description("Refresh the organisation cache")
      .consumes(PLAIN)
      .produces(PLAIN)
      .responseMessage()
      .code(200)
      .message("Command accepted")
      .endResponseMessage()
      .to("direct:adminRefreshOrganisationCache");

    from("direct:adminRouteAuthorizeGet")
      .throwException(new NotFoundException())
      .routeId("admin-route-authorize-get");

    from("direct:adminRouteAuthorizePost")
      .throwException(new NotFoundException())
      .routeId("admin-route-authorize-post");

    from("direct:adminRouteAuthorizePut")
      .throwException(new NotFoundException())
      .routeId("admin-route-authorize-put");

    from("direct:adminRouteAuthorizeDelete")
      .throwException(new NotFoundException())
      .routeId("admin-route-authorize-delete");

    from("direct:validationReport")
      .to("direct:authorizeEditorRequest")
      .setHeader(
        FILE_HANDLE,
        constant(BLOBSTORE_PATH_ANTU_REPORTS)
          .append(header(CODESPACE_PARAM))
          .append(Constants.VALIDATION_REPORT_PREFIX)
          .append(header(VALIDATION_REPORT_ID_PARAM))
          .append(VALIDATION_REPORT_SUFFIX)
      )
      .log(
        LoggingLevel.INFO,
        correlation() +
        "Downloading NeTEx validation report ${header." +
        FILE_HANDLE +
        "}"
      )
      .process(this::removeAllCamelHttpHeaders)
      .to("direct:getAntuBlob")
      .filter(simple("${body} == null"))
      .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
      // end filter
      .end()
      .setHeader(Exchange.CONTENT_ENCODING, constant("gzip"))
      .removeHeader(HttpHeaders.AUTHORIZATION)
      .routeId("validation-report");

    from("direct:adminCacheClear")
      .to("direct:authorizeAdminRequest")
      .log(LoggingLevel.INFO, correlation() + "Clear cache")
      .process(this::removeAllCamelHttpHeaders)
      .bean("cacheAdmin", "clear")
      .log(LoggingLevel.INFO, correlation() + "Cleared cache")
      .routeId("admin-cache-clear");

    from("direct:adminCacheDeleteByPattern")
      .to("direct:authorizeAdminRequest")
      .log(LoggingLevel.INFO, correlation() + "Deleting key by pattern")
      .process(this::removeAllCamelHttpHeaders)
      .bean(
        "cacheAdmin",
        "deleteKeysByPattern(${header." + PATTERN_PARAM + "})"
      )
      .log(LoggingLevel.INFO, correlation() + "Deleted key by pattern")
      .routeId("admin-delete-key-by-pattern");

    from("direct:adminCacheDumpKeys")
      .to("direct:authorizeAdminRequest")
      .log(LoggingLevel.INFO, correlation() + "Dump keys")
      .process(this::removeAllCamelHttpHeaders)
      .bean("cacheAdmin", "dumpKeys")
      .log(LoggingLevel.INFO, correlation() + "Dumped keys")
      .routeId("admin-cache-dump-keys");

    from("direct:adminRefreshStopCache")
      .to("direct:authorizeAdminRequest")
      .log(LoggingLevel.INFO, correlation() + "Scheduling stop cache refresh")
      .process(this::removeAllCamelHttpHeaders)
      .to("direct:scheduleRefreshStopCache")
      .log(LoggingLevel.INFO, correlation() + "Scheduled stop cache refresh")
      .routeId("admin-refresh-stop-cache");

    from("direct:adminRefreshOrganisationCache")
      .to("direct:authorizeAdminRequest")
      .log(
        LoggingLevel.INFO,
        correlation() + "Scheduling organisation cache refresh"
      )
      .process(this::removeAllCamelHttpHeaders)
      .to("direct:scheduleRefreshOrganisationCache")
      .log(
        LoggingLevel.INFO,
        correlation() + "Scheduled organisation cache refresh"
      )
      .routeId("admin-refresh-organisation-cache");

    from("direct:authorizeEditorRequest")
      .validate(header(CODESPACE_PARAM).isNotNull())
      .doTry()
      .bean(
        antuAuthorizationService,
        "verifyRouteDataEditorPrivileges(${header." + CODESPACE_PARAM + "})"
      )
      .routeId("admin-authorize-editor-request");

    from("direct:authorizeAdminRequest")
      .doTry()
      .process(e -> antuAuthorizationService.verifyAdministratorPrivileges())
      .routeId("admin-authorize-admin-request");
  }
}
