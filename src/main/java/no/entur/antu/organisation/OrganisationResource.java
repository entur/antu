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
 */

package no.entur.antu.organisation;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * REST-client to access the Organisation Register REST-service.
 */
public class OrganisationResource {

    private static final long MAX_RETRY_ATTEMPTS = 3;
    private static final int HTTP_TIMEOUT = 10000;
    public static final int MAX_IDLE_TIME = 20;
    public static final int MAX_LIFE_TIME = 60;
    public static final int PENDING_ACQUIRE_TIMEOUT = 60;
    public static final int EVICT_IN_BACKGROUND = 120;


    private final WebClient webClient;

    public OrganisationResource(String organisationRegistryUrl, WebClient orgRegisterClient) {

        ConnectionProvider provider = ConnectionProvider.builder("provider")
                .maxIdleTime(Duration.ofSeconds(MAX_IDLE_TIME))
                .maxLifeTime(Duration.ofSeconds(MAX_LIFE_TIME))
                .pendingAcquireTimeout(Duration.ofSeconds(PENDING_ACQUIRE_TIMEOUT))
                .evictInBackground(Duration.ofSeconds(EVICT_IN_BACKGROUND)).build();

        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, HTTP_TIMEOUT).doOnConnected(connection -> {
            connection.addHandlerLast(new ReadTimeoutHandler(HTTP_TIMEOUT, TimeUnit.MILLISECONDS));
            connection.addHandlerLast(new WriteTimeoutHandler(HTTP_TIMEOUT, TimeUnit.MILLISECONDS));
        }));
        this.webClient = orgRegisterClient.mutate()
                .defaultHeader("Et-Client-Name", "entur-antu")
                .clientConnector(connector)
                .baseUrl(organisationRegistryUrl)
                .build();
    }

    public Collection<Organisation> getOrganisations() {

        return webClient.get()
                .retrieve()
                .bodyToFlux(Organisation.class)
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(is5xx))
                .collectList().block();

    }

    protected static final Predicate<Throwable> is5xx =
            throwable -> throwable instanceof WebClientResponseException webClientResponseException && webClientResponseException.getStatusCode().is5xxServerError();


}
