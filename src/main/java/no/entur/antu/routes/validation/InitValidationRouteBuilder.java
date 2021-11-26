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


import no.entur.antu.Constants;
import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.springframework.stereotype.Component;

import static no.entur.antu.Constants.JOB_TYPE;
import static no.entur.antu.Constants.JOB_TYPE_AGGREGATE;
import static no.entur.antu.Constants.JOB_TYPE_SPLIT;
import static no.entur.antu.Constants.JOB_TYPE_VALIDATE;
import static no.entur.antu.Constants.STATUS_VALIDATION_STARTED;


/**
 * Process NeTEX validation requests from Marduk.
 */
@Component
public class InitValidationRouteBuilder extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {
        super.configure();


        from("google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationQueue")
                .to("direct:initDatasetValidation")
                .routeId("netex-validation-queue-pubsub");

        from("direct:initDatasetValidation")
                .setBody(constant(STATUS_VALIDATION_STARTED))
                .to("direct:notifyMarduk")
                .setHeader(Constants.VALIDATION_REPORT_ID, simple("${date:now:yyyyMMddHHmmssSSS}"))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_SPLIT))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                .routeId("init-dataset-validation");

        // pulling synchronously with one consumer to ensure that jobs are processed one by one.
        from("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue?synchronousPull=true")
                .to("direct:processJob")
                .routeId("process-job-queue-pubsub");

        from("direct:processJob")
                .choice()
                .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_SPLIT))
                .to("direct:splitDataset")
                .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_VALIDATE))
                .to("direct:validateNetex")
                .when(header(JOB_TYPE).isEqualTo(JOB_TYPE_AGGREGATE))
                .to("direct:aggregateReports")
                .otherwise()
                .log(LoggingLevel.ERROR, correlation() + "Unknown job type ${header." + Constants.JOB_TYPE + " } ")
                .routeId("process-job");

        from("direct:notifyMarduk")
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuNetexValidationStatusQueue")
                .routeId("notify-marduk");


    }
}
