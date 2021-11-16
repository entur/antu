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

package no.entur.antu.routes.validation;


import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.entur.antu.Constants.CODESPACE;


/**
 * Receive a notification when a new NeTEx export is available in the blob store and convert it into a GTFS dataset.
 */
@Component
public class NeTExValidationQueueRouteBuilder extends BaseRouteBuilder {

    private static final String TIMETABLE_DATASET_FILE = "TIMETABLE_DATASET_FILE";

    public static final String STATUS_VALIDATION_STARTED = "started";
    public static final String STATUS_VALIDATION_OK = "ok";
    public static final String STATUS_VALIDATION_FAILED = "failed";

    @Override
    public void configure() throws Exception {
        super.configure();


        from("google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationQueue?synchronousPull=true")
                .to("direct:netexValidationQueue")
                .routeId("netex-validation-queue-pubsub");

        from("direct:netexValidationQueue")
                .process(this::setCorrelationIdIfMissing)
                .setHeader(CODESPACE, bodyAs(String.class))
                .log(LoggingLevel.INFO, correlation() + "Received NeTEx validation request")

                .setBody(constant(STATUS_VALIDATION_STARTED))
                .to("direct:notifyMarduk")

                .doTry()
                .to("direct:downloadNetexDataset")
                .log(LoggingLevel.INFO, correlation() + "NeTEx Timetable file downloaded")
                .setHeader(TIMETABLE_DATASET_FILE, body())
                .to("direct:validateNetexDataset")
                .setBody(constant(STATUS_VALIDATION_OK))
                .to("direct:notifyMarduk")
                .doCatch(Exception.class)
                .log(LoggingLevel.ERROR, correlation() + "Dataset processing failed: ${exception.message} stacktrace: ${exception.stacktrace}")
                .setBody(constant(STATUS_VALIDATION_FAILED))
                .to("direct:notifyMarduk")
                .routeId("netex-validation-queue");

        from("direct:downloadNetexDataset")
                .log(LoggingLevel.INFO, correlation() + "Downloading NeTEx dataset")
                .to("direct:getMardukBlob")
                .filter(body().isNull())
                .log(LoggingLevel.ERROR, correlation() + "NeTEx file not found")
                .stop()
                //end filter
                .end()
                .routeId("download-netex-timetable-dataset");

        from("direct:validateNetexDataset")
                .log(LoggingLevel.INFO, correlation() + "Validating NeTEx dataset")
                .bean("authorityIdValidator", "validateAuthorityId(${body},${header." + CODESPACE + "})")
                .routeId("validate-netex-dataset");

        from("direct:notifyMarduk")
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationStatusQueue")
                .routeId("notify-marduk");
    }
}
