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


import no.entur.antu.routes.BaseRouteBuilder;
import org.apache.camel.LoggingLevel;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.springframework.stereotype.Component;


/**
 * Aggregate validation reports.
 */
@Component
public class ValidationReportsAggregationRouteBuilder extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {
        super.configure();

        from("master:lockOnAntuReportAggregationQueue:google-pubsub:{{antu.pubsub.project.id}}:AntuReportAggregationQueue")
                .process(this::removeSynchronizationForAggregatedExchange)
                .aggregate(simple("true", Boolean.class)).aggregationStrategy(new GroupedMessageAggregationStrategy()).completionTimeout(5000)
                .process(this::addSynchronizationForAggregatedExchange)
                .process(this::setNewCorrelationId)
                .log(LoggingLevel.INFO, correlation() +  "Aggregated ${exchangeProperty.CamelAggregatedSize} validation reports (aggregation completion triggered by ${exchangeProperty.CamelAggregatedCompletedBy}).")
                .to("direct:uploadAggregatedReport")
                .routeId("aggregate-reports-pubsub");

        from("direct:aggregateReports")
                .log(LoggingLevel.INFO, correlation() + "Aggregating reports")
                .routeId("aggregate-reports");



        from("direct:uploadAggregatedReport")
                .log(LoggingLevel.INFO, correlation() + "Uploading aggregated validation report")
                .routeId("upload-aggregated-report");


    }
}
