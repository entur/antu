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

package no.entur.antu.routes;

import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.ProjectSubscriptionName;
import no.entur.antu.Constants;
import org.apache.camel.Exchange;
import org.apache.camel.ExtendedExchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
import org.apache.camel.component.google.pubsub.GooglePubsubEndpoint;
import org.apache.camel.component.google.pubsub.consumer.AcknowledgeAsync;
import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.apache.camel.support.DefaultExchange;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static no.entur.antu.Constants.NETEX_FILE_NAME;

/**
 * Defines common route behavior.
 */
public abstract class BaseRouteBuilder extends RouteBuilder {

    private static final int ACK_DEADLINE_EXTENSION = 600;
    private static final String SYNCHRONIZATION_HOLDER = "SYNCHRONIZATION_HOLDER";

    /**
     * Only these headers are copied from the Camel message to a PubSub topic.
     */
    private static final String[] PUBSUB_OUTBOUND_HEADERS_WHITELIST = {
            Constants.CORRELATION_ID,
            Constants.DATASET_REFERENTIAL,
            Constants.DATASET_CODESPACE,
            Constants.DATASET_NB_COMMON_FILES,
            Constants.DATASET_NB_NETEX_FILES,
            Constants.FILE_HANDLE, NETEX_FILE_NAME,
            Constants.JOB_TYPE,
            Constants.VALIDATION_REPORT_ID,
            Constants.VALIDATION_STAGE_HEADER,
            Constants.VALIDATION_CLIENT_HEADER,
            Constants.ENCRYPTION_KEY};

    @Value("${quartz.lenient.fire.time.ms:180000}")
    private int lenientFireTimeMs;

    @Value("${antu.camel.redelivery.max:3}")
    private int maxRedelivery;

    @Value("${antu.camel.redelivery.delay:5000}")
    private int redeliveryDelay;

    @Value("${antu.camel.redelivery.backoff.multiplier:3}")
    private int backOffMultiplier;

    @Override
    public void configure() throws Exception {
        errorHandler(defaultErrorHandler()
                .redeliveryDelay(redeliveryDelay)
                .maximumRedeliveries(maxRedelivery)
                .onRedelivery(this::logRedelivery)
                .useExponentialBackOff()
                .backOffMultiplier(backOffMultiplier)
                .logExhausted(true)
                .logRetryStackTrace(true));


        // Copy all PubSub headers except the internal Camel PubSub headers from the PubSub message into the Camel message headers.
        interceptFrom(".*google-pubsub:.*")
                .process(exchange ->
                {
                    Map<String, String> pubSubAttributes = exchange.getIn().getHeader(GooglePubsubConstants.ATTRIBUTES, Map.class);
                    if (pubSubAttributes == null) {
                        throw new IllegalStateException("Missing PubSub attribute maps in Exchange");
                    }
                    pubSubAttributes.entrySet()
                            .stream()
                            .filter(entry -> !entry.getKey().startsWith("CamelGooglePubsub"))
                            .forEach(entry -> exchange.getIn().setHeader(entry.getKey(), entry.getValue()));
                });

        // Copy only a whitelist of message headers from the Camel exchange into the PubSub message.
        interceptSendToEndpoint("google-pubsub:*").process(
                exchange -> {
                    Map<String, String> pubSubAttributes = new HashMap<>(exchange.getIn().getHeader(GooglePubsubConstants.ATTRIBUTES, new HashMap<>(), Map.class));

                    Stream.of(PUBSUB_OUTBOUND_HEADERS_WHITELIST).forEach(header -> {
                                if (exchange.getIn().getHeader(header) != null) {
                                    pubSubAttributes.put(header, exchange.getIn().getHeader(header, String.class));
                                }
                            }
                    );

                    exchange.getIn().setHeader(GooglePubsubConstants.ATTRIBUTES, pubSubAttributes);
                });

    }

    protected void logRedelivery(Exchange exchange) {
        int redeliveryCounter = exchange.getIn().getHeader("CamelRedeliveryCounter", Integer.class);
        int redeliveryMaxCounter = exchange.getIn().getHeader("CamelRedeliveryMaxCounter", Integer.class);
        Throwable camelCaughtThrowable = exchange.getProperty("CamelExceptionCaught", Throwable.class);
        String correlation = simple(correlation(), String.class).evaluate(exchange, String.class);

        log.warn("{} Exchange failed, redelivering the message locally, attempt {}/{}...", correlation, redeliveryCounter, redeliveryMaxCounter, camelCaughtThrowable);
    }

    protected String logDebugShowAll() {
        return "log:" + getClass().getName() + "?level=DEBUG&showAll=true&multiline=true";
    }

    protected void setNewCorrelationId(Exchange e) {
        e.getIn().setHeader(Constants.CORRELATION_ID, UUID.randomUUID().toString());
    }

    protected void setCorrelationIdIfMissing(Exchange e) {
        e.getIn().setHeader(Constants.CORRELATION_ID, e.getIn().getHeader(Constants.CORRELATION_ID, UUID.randomUUID().toString()));
    }

    protected String correlation() {
        return "[referential=${header." + Constants.DATASET_REFERENTIAL + "} reportId=${header." + Constants.VALIDATION_REPORT_ID + "} fileName= ${header." + NETEX_FILE_NAME  + "} correlationId=${header." + Constants.CORRELATION_ID + "}] ";
    }

    public void extendAckDeadline(Exchange exchange) throws IOException {
        String ackId = exchange.getIn().getHeader(GooglePubsubConstants.ACK_ID, String.class);
        GooglePubsubEndpoint fromEndpoint = (GooglePubsubEndpoint) exchange.getFromEndpoint();
        String subscriptionName = ProjectSubscriptionName.format(fromEndpoint.getProjectId(), fromEndpoint.getDestinationName());
        ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest.newBuilder()
                .setSubscription(subscriptionName)
                .addAllAckIds(List.of(ackId))
                .setAckDeadlineSeconds(ACK_DEADLINE_EXTENSION)
                .build();
        try (SubscriberStub subscriberStub = fromEndpoint.getComponent().getSubscriberStub(fromEndpoint.getServiceAccountKey())) {
            subscriberStub.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
        }
    }

    /**
     * Remove the PubSub synchronization.
     * This prevents an aggregator from acknowledging the aggregated PubSub messages before the end of the route.
     * In case of failure during the routing this would make it impossible to retry the messages.
     * The synchronization is stored temporarily in a header and is applied again after the aggregation is complete
     *
     * @param e
     * @see #addSynchronizationForAggregatedExchange(Exchange)
     */
    public void removeSynchronizationForAggregatedExchange(Exchange e) {
        DefaultExchange temporaryExchange = new DefaultExchange(e.getContext());
        e.getUnitOfWork().handoverSynchronization(temporaryExchange, AcknowledgeAsync.class::isInstance);
        e.getIn().setHeader(SYNCHRONIZATION_HOLDER, temporaryExchange);
    }

    /**
     * Add back the PubSub synchronization.
     *
     * @see #removeSynchronizationForAggregatedExchange(Exchange)
     */
    protected void addSynchronizationForAggregatedExchange(Exchange aggregatedExchange) {
        List<Message> messages = aggregatedExchange.getIn().getBody(List.class);
        for (Message m : messages) {
            Exchange temporaryExchange = m.getHeader(SYNCHRONIZATION_HOLDER, Exchange.class);
            if (temporaryExchange == null) {
                throw new IllegalStateException("Synchronization holder not found");
            }
            temporaryExchange.adapt(ExtendedExchange.class).handoverCompletions(aggregatedExchange);
        }
    }

    protected void removeAllCamelHttpHeaders(Exchange e) {
        e.getIn().removeHeaders(Constants.CAMEL_ALL_HTTP_HEADERS);
    }
}
