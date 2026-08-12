package no.entur.antu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * The application, minus the security auto-configuration the tests do not want.
 *
 * <p>The explicit ComponentScan has to repeat the two filters SpringBootApplication would otherwise
 * contribute. Without TypeExcludeFilter, every {@code @TestConfiguration} under {@code no.entur.antu}
 * is picked up by every test that boots this class, so one test's doubles silently replace another
 * test's beans.
 */
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@ComponentScan(
  excludeFilters = {
    @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = App.class),
    @ComponentScan.Filter(
      type = FilterType.CUSTOM,
      classes = TypeExcludeFilter.class
    ),
    @ComponentScan.Filter(
      type = FilterType.CUSTOM,
      classes = AutoConfigurationExcludeFilter.class
    ),
  }
)
public class TestApp extends App {

  public static void main(String[] args) {
    SpringApplication.run(TestApp.class, args);
  }
}
