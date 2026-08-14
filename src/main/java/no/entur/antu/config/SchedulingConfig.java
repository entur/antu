package no.entur.antu.config;

import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * The scheduler behind the {@code @Scheduled} methods: the leader heartbeat, the two cache refresh
 * triggers and the stalled-validation sweep.
 *
 * <p>Spring Boot would normally contribute this, but its auto-configuration backs off when another
 * {@code TaskScheduler} bean already exists, and spring-cloud-gcp registers several: the publisher
 * pool, the global subscriber pool, and one per subscription. Without a bean named
 * {@code taskScheduler}, {@code @Scheduled} falls back to a single-threaded executor of its own and
 * silently ignores {@code spring.task.scheduling.pool.size}. It says so once per boot, at INFO, from
 * {@code TaskSchedulerRouter}, which is why this went unnoticed for a release.
 */
@Configuration
public class SchedulingConfig {

  @Bean
  ThreadPoolTaskScheduler taskScheduler(
    ThreadPoolTaskSchedulerBuilder builder
  ) {
    return builder.build();
  }
}
