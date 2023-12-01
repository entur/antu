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

package no.entur.antu.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Common network timeout configuration for web clients.
 */
@Configuration
public class WebClientConfig {

    private static final int HTTP_TIMEOUT = 10000;
    public static final int MAX_IDLE_TIME = 20;
    public static final int MAX_LIFE_TIME = 60;
    public static final int PENDING_ACQUIRE_TIMEOUT = 60;
    public static final int EVICT_IN_BACKGROUND = 120;


    @Bean
    ClientHttpConnector webClientHttpConnector() {
        ConnectionProvider provider = ConnectionProvider.builder("provider")
                .maxIdleTime(Duration.ofSeconds(MAX_IDLE_TIME))
                .maxLifeTime(Duration.ofSeconds(MAX_LIFE_TIME))
                .pendingAcquireTimeout(Duration.ofSeconds(PENDING_ACQUIRE_TIMEOUT))
                .evictInBackground(Duration.ofSeconds(EVICT_IN_BACKGROUND)).build();

        return new ReactorClientHttpConnector(HttpClient.create(provider).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, HTTP_TIMEOUT).doOnConnected(connection -> {
            connection.addHandlerLast(new ReadTimeoutHandler(HTTP_TIMEOUT, TimeUnit.MILLISECONDS));
            connection.addHandlerLast(new WriteTimeoutHandler(HTTP_TIMEOUT, TimeUnit.MILLISECONDS));
        }));
    }
}
