package no.entur.antu.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.entur.antu.TestApp;
import no.entur.antu.pipeline.PipelineTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Declared with the same context configuration as {@code AntuPipelineTestBase} on purpose, so it shares
 * that cached context. {@code @SpringBootTest(properties = ...)} would fork a second one.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class
)
@Import({ TestConfig.class, PipelineTestConfig.class })
@ActiveProfiles({ "test", "default", "in-memory-blobstore" })
class SchedulingConfigTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Value("${spring.task.scheduling.pool.size}")
  private int configuredPoolSize;

  @Test
  void scheduledTasksRunOnTheConfiguredPool() {
    TaskScheduler scheduler = applicationContext.getBean(
      "taskScheduler",
      TaskScheduler.class
    );

    ThreadPoolTaskScheduler threadPool = assertInstanceOf(
      ThreadPoolTaskScheduler.class,
      scheduler
    );
    assertEquals(
      configuredPoolSize,
      threadPool.getScheduledThreadPoolExecutor().getCorePoolSize(),
      "spring.task.scheduling.pool.size must reach the scheduler @Scheduled uses"
    );
  }

  /**
   * The pool size is chosen as one thread per {@code @Scheduled} method. Nothing but this enforces it,
   * and a fifth method would silently reintroduce the queueing the pool size exists to prevent.
   */
  @Test
  void everyScheduledMethodHasAThread() {
    ScheduledTaskHolder taskHolder = applicationContext.getBean(
      ScheduledTaskHolder.class
    );

    assertTrue(
      taskHolder.getScheduledTasks().size() <= configuredPoolSize,
      "every @Scheduled method needs a thread, or the leader heartbeat queues behind a cache refresh"
    );
  }
}
