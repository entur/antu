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
import no.entur.antu.exception.AntuException;
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

import static no.entur.antu.Constants.CORRELATION_ID;
import static no.entur.antu.Constants.DATASET_CODESPACE;
import static no.entur.antu.Constants.DATASET_NB_COMMON_FILES;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.FILENAME_DELIMITER;
import static no.entur.antu.Constants.FILE_HANDLE;
import static no.entur.antu.Constants.JOB_TYPE_AGGREGATE_COMMON_FILES;
import static no.entur.antu.Constants.JOB_TYPE_VALIDATE;
import static no.entur.antu.Constants.NETEX_FILE_NAME;
import static no.entur.antu.Constants.VALIDATION_CLIENT_HEADER;
import static no.entur.antu.Constants.VALIDATION_PROFILE_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID;
import static no.entur.antu.Constants.VALIDATION_STAGE_HEADER;


/**
 * Barrier waiting for all common files to be validated before creating validation jobs for the line files.
 * Some validation rules applied to line files require that common files are processed first.
 */
@Component
public class CommonFilesBarrierRouteBuilder extends BaseRouteBuilder {

    private static final String PROP_DATASET_NETEX_FILE_NAMES = "EnturDatasetNetexFileNames";

    @Override
    public void configure() throws Exception {
        super.configure();

        from("master:lockOnAntuCommonFilesAggregationQueue:google-pubsub:{{antu.pubsub.project.id}}:AntuCommonFilesAggregationQueue")
                .process(this::removeSynchronizationForAggregatedExchange)
                .aggregate(header(VALIDATION_REPORT_ID)).aggregationStrategy(new CommonFilesAggregationStrategy()).completionTimeout(1800000)
                .process(this::addSynchronizationForAggregatedExchange)
                .log(LoggingLevel.INFO, correlation() + "Aggregated ${exchangeProperty.CamelAggregatedSize} common files (aggregation completion triggered by ${exchangeProperty.CamelAggregatedCompletedBy}).")
                .setBody(exchangeProperty(PROP_DATASET_NETEX_FILE_NAMES))
                .setHeader(Constants.JOB_TYPE, simple(JOB_TYPE_AGGREGATE_COMMON_FILES))
                .to("google-pubsub:{{antu.pubsub.project.id}}:AntuJobQueue")
                .routeId("aggregate-common-files-pubsub");

        from("direct:createLineFilesValidationJobs")
                .convertBodyTo(String.class)
                .split(body()).delimiter(FILENAME_DELIMITER)
                .filter(PredicateBuilder.not(body().startsWith("_")))
                .log(LoggingLevel.DEBUG, correlation() + "Creating validation job for file ${body}")
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
     * Complete the aggregation when all common files have been received.
     * The total number of common files to process is stored in a header that is included in every incoming message.
     */
    private static class CommonFilesAggregationStrategy extends GroupedMessageAggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            Exchange aggregatedExchange = super.aggregate(oldExchange, newExchange);
            aggregatedExchange.getIn().setHeader(VALIDATION_REPORT_ID, newExchange.getIn().getHeader(VALIDATION_REPORT_ID));
            aggregatedExchange.getIn().setHeader(DATASET_CODESPACE, newExchange.getIn().getHeader(DATASET_CODESPACE));
            aggregatedExchange.getIn().setHeader(DATASET_REFERENTIAL, newExchange.getIn().getHeader(DATASET_REFERENTIAL));
            aggregatedExchange.getIn().setHeader(CORRELATION_ID, newExchange.getIn().getHeader(CORRELATION_ID));
            aggregatedExchange.getIn().setHeader(VALIDATION_STAGE_HEADER, newExchange.getIn().getHeader(VALIDATION_STAGE_HEADER));
            aggregatedExchange.getIn().setHeader(VALIDATION_CLIENT_HEADER, newExchange.getIn().getHeader(VALIDATION_CLIENT_HEADER));
            aggregatedExchange.getIn().setHeader(VALIDATION_PROFILE_HEADER, newExchange.getIn().getHeader(VALIDATION_PROFILE_HEADER));
            aggregatedExchange.setProperty(PROP_DATASET_NETEX_FILE_NAMES, newExchange.getIn().getBody());
            // check if all individual reports have been received
            // checking against the set of distinct file names in order to exclude possible multiple redeliveries of the same report.
            Long nbCommonFiles = newExchange.getIn().getHeader(DATASET_NB_COMMON_FILES, Long.class);
            if (nbCommonFiles == null) {
                throw new AntuException("Header not found: " + DATASET_NB_COMMON_FILES + " for validation report " + newExchange.getIn().getHeader(VALIDATION_REPORT_ID) + " and codespace " + newExchange.getIn().getHeader(DATASET_CODESPACE));
            }
            List<Message> aggregatedMessages = aggregatedExchange.getProperty(ExchangePropertyKey.GROUPED_EXCHANGE, List.class);
            Set<String> aggregatedFileNames = aggregatedMessages.stream().map(message -> message.getHeader(NETEX_FILE_NAME, String.class)).collect(Collectors.toSet());
            if (aggregatedFileNames.size() >= nbCommonFiles) {
                aggregatedExchange.setProperty(Exchange.AGGREGATION_COMPLETE_CURRENT_GROUP, true);
            }
            return aggregatedExchange;
        }
    }

}
