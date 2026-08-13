package no.entur.antu.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {

  /**
   * The mapper validation reports are written and read with.
   *
   * <p>Timestamps are ISO-8601 strings and null fields are left out, which is the shape the published
   * reports have always had and what their consumers parse. Kept separate from the mapper Spring uses
   * for the web layer so that changing one cannot alter the report format.
   */
  @Bean("validationReportObjectMapper")
  public ObjectMapper validationReportObjectMapper() {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(
      LocalDateTime.class,
      new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME)
    );
    javaTimeModule.addSerializer(
      LocalDateTime.class,
      new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME)
    );
    return JsonMapper
      .builder()
      .addModule(javaTimeModule)
      .serializationInclusion(JsonInclude.Include.NON_NULL)
      .build();
  }
}
