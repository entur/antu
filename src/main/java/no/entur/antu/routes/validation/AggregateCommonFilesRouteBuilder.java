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
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePropertyKey;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.processor.aggregate.GroupedMessageAggregationStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.DATASET_NB_COMMON_FILES;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.JOB_TYPE_AGGREGATE_COMMON_FILES;
import static no.entur.antu.Constants.JOB_TYPE_VALIDATE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;
import static no.entur.antu.routes.validation.SplitDatasetRouteBuilder.ALL_NETEX_FILE_NAMES;


/**
 * Aggregate validation reports.
 */
@Component
public class AggregateCommonFilesRouteBuilder extends BaseRouteBuilder {

    @Override
    public void configure() throws Exception {
        super.configure();

        from("master:lockOnAntuCommonFilesAggregationQueue:google-pubsub:{{antu.pubsub.project.id}}:AntuCommonFilesAggregationQueue")
                .process(this::removeSynchronizationForAggregatedExchange)
                .aggregate(header(VALIDATION_REPORT_ID)).aggregationStrategy(new CommonFilesAggregationStrategy()).completionTimeout(1800000)
                .process(this::addSynchronizationForAggregatedExchange)
                .process(this::setNewCorrelationId)
                .log(LoggingLevel.INFO, correlation() + "Aggregated ${exchangeProperty.CamelAggregatedSize} common files (aggregation completion triggered by ${exchangeProperty.CamelAggregatedCompletedBy}).")
                .process(exchange -> System.out.println("aa"))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_AGGREGATE_COMMON_FILES))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                .routeId("aggregate-common-files-pubsub");

        from("direct:createLineFilesValidationJobs")
                .split(header(ALL_NETEX_FILE_NAMES))
                .filter(PredicateBuilder.not(body().startsWith("_")))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_VALIDATE))
                .setHeader(Constants.DATASET_NB_NETEX_FILES, exchangeProperty(Exchange.SPLIT_SIZE))
                .setHeader(NETEX_FILE_NAME, body())
                .setHeader(FILE_HANDLE, simple(Constants.GCS_BUCKET_FILE_NAME))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                //end filter
                .end()
                //end split
                .end()
                .routeId("create-line-files-validation-jobs");
    }

    /**
     * Complete the aggregation when all the individual reports have been received.
     * The total number of reports to process is stored in a header that is included in every incoming message.
     */
    private static class CommonFilesAggregationStrategy extends GroupedMessageAggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            Exchange aggregatedExchange = super.aggregate(oldExchange, newExchange);
            aggregatedExchange.getIn().setHeader(VALIDATION_REPORT_ID, newExchange.getIn().getHeader(VALIDATION_REPORT_ID));
            aggregatedExchange.getIn().setHeader(DATASET_CODESPACE, newExchange.getIn().getHeader(DATASET_CODESPACE));
            aggregatedExchange.getIn().setBody(newExchange.getIn().getBody());
            // check if all individual reports have been received
            // checking against the set of distinct file names in order to exclude possible multiple redeliveries of the same report.
            Long nbCommonFiles = newExchange.getIn().getHeader(DATASET_NB_COMMON_FILES, Long.class);
            List<Message> aggregatedMessages = aggregatedExchange.getProperty(ExchangePropertyKey.GROUPED_EXCHANGE, List.class);
            Set<String> aggregatedFileNames = aggregatedMessages.stream().map(message -> message.getHeader(NETEX_FILE_NAME, String.class)).collect(Collectors.toSet());
            if (aggregatedFileNames.size() >= nbCommonFiles) {
                aggregatedExchange.setProperty(Exchange.AGGREGATION_COMPLETE_CURRENT_GROUP, true);
            }
            return aggregatedExchange;
        }
    }

}
