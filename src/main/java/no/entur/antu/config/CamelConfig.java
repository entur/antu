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

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.camel.component.google.pubsub.GooglePubsubComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {

    /**
     * Add the DEADLINE_EXCEEDED error code to the list of retryable PubSub server errors.
     *
     * @return a customized Google PubSub component that can retry DEADLINE_EXCEEDED errors.
     */
    @Bean("google-pubsub")
    public GooglePubsubComponent googlePubsubComponent() {
        GooglePubsubComponent googlePubsubComponent = new GooglePubsubComponent();
        googlePubsubComponent.setSynchronousPullRetryableCodes("DEADLINE_EXCEEDED");
        return googlePubsubComponent;
    }

    /**
     * Register Java Time Module for JSON serialization/deserialization of Java Time objects.
     * @return
     */
    @Bean("jacksonJavaTimeModule")
    JavaTimeModule jacksonJavaTimeModule() {
        return new JavaTimeModule();
    }
}
