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

import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import no.entur.antu.security.AuthorizationService;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import static no.entur.antu.Constants.BLOBSTORE_PATH_REPORTS_SUBDIR;
import static no.entur.antu.Constants.FILE_HANDLE;

@Component
public class RestValidationReportRouteBuilder extends BaseRouteBuilder {


    private static final String PLAIN = "text/plain";
    private static final String JSON = "application/json";
    private static final String SWAGGER_DATA_TYPE_STRING = "string";
    private static final String CODESPACE_PARAM = "codespace";
    private static final String VALIDATION_REPORT_ID_PARAM = "id";

    private final AuthorizationService authorizationService;
    private final String host;
    private final String port;

    public RestValidationReportRouteBuilder(AuthorizationService authorizationService, @Value("${server.host:0.0.0.0}") String host, @Value("${server.port:8080}") String port) {
        this.authorizationService = authorizationService;
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
                .component("servlet")
                .contextPath("/services")
                .bindingMode(RestBindingMode.off)
                .endpointProperty("matchOnUriPrefix", "true")
                .apiContextPath("/swagger.json")
                .apiProperty("api.title", "Antu NeTEx Validation API").apiProperty("api.version", "1.0")
                .apiContextRouteId("doc-api");

        rest("")
                .apiDocs(false)
                .description("Wildcard definitions necessary to get Jetty to match authorization filters to endpoints with path params")
                .get().route().routeId("admin-route-authorize-get").throwException(new NotFoundException()).endRest()
                .post().route().routeId("admin-route-authorize-post").throwException(new NotFoundException()).endRest()
                .put().route().routeId("admin-route-authorize-put").throwException(new NotFoundException()).endRest()
                .delete().route().routeId("admin-route-authorize-delete").throwException(new NotFoundException()).endRest();

        String commonApiDocEndpoint = "http:" + host + ":" + port + "/services/swagger.json?bridgeEndpoint=true";

        rest("/validation-report")

                .get("/{" + CODESPACE_PARAM + "}/{" + VALIDATION_REPORT_ID_PARAM + '}')
                .description("Return the validation report for a given validation report id")
                .param().name(CODESPACE_PARAM)
                .type(RestParamType.path)
                .description("Provider Codespace")
                .dataType(SWAGGER_DATA_TYPE_STRING)
                .required(true)
                .endParam()
                .param().name(VALIDATION_REPORT_ID_PARAM)
                .type(RestParamType.path)
                .description("Unique Id of the validation report")
                .dataType(SWAGGER_DATA_TYPE_STRING)
                .required(true)
                .endParam()
                .consumes(PLAIN)
                .produces(JSON)
                .responseMessage().code(200).endResponseMessage()
                .responseMessage().code(404).message("Unknown codespace").endResponseMessage()
                .responseMessage().code(500).message("Internal error").endResponseMessage()
                .route()
                .to("direct:authorizeEditorRequest")
                .setHeader(FILE_HANDLE, constant(BLOBSTORE_PATH_REPORTS_SUBDIR)
                        .append(header(CODESPACE_PARAM))
                        .append(Constants.VALIDATION_REPORT_PREFIX)
                        .append(header(VALIDATION_REPORT_ID_PARAM))
                        .append(".json"))
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx validation report ${header." + FILE_HANDLE  + "}")
                .process(this::removeAllCamelHttpHeaders)
                .to("direct:getAntuBlob")
                .filter(simple("${body} == null"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                .routeId("validation-report")
                .endRest()


                .get("/swagger.json")
                .apiDocs(false)
                .bindingMode(RestBindingMode.off)
                .route()
                .to(commonApiDocEndpoint)
                .endRest();


        from("direct:authorizeEditorRequest")
                .doTry()
                .bean(authorizationService, "verifyRouteDataEditorPrivileges(${header." + CODESPACE_PARAM + "})" )
                .routeId("admin-authorize-editor-request");
    }

}
