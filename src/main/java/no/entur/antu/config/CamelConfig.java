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
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import no.entur.antu.memorystore.RedisTemporaryFileRepository;
import no.entur.antu.memorystore.TemporaryFileRepository;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelConfig {

  /**
   * Register Java Time Module for JSON serialization/deserialization of Java Time objects.
   *
   * @return
   */
  @Bean("jacksonJavaTimeModule")
  JavaTimeModule jacksonJavaTimeModule() {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(
      LocalDateTime.class,
      new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME)
    );
    javaTimeModule.addSerializer(
      LocalDateTime.class,
      new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME)
    );
    return javaTimeModule;
  }

  @Bean
  TemporaryFileRepository temporaryFileRepository(
    RedissonClient redissonClient
  ) {
    return new RedisTemporaryFileRepository(redissonClient);
  }
}
