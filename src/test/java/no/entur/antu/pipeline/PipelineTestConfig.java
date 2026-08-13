package no.entur.antu.pipeline;

import no.entur.antu.job.JobDispatcher;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationStatusNotifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class PipelineTestConfig {

  @Bean
  @Primary
  RecordingJobQueue recordingJobQueue(
    ObjectProvider<JobDispatcher> jobDispatcher
  ) {
    return new RecordingJobQueue(jobDispatcher);
  }

  @Bean
  @Primary
  RecordingValidationStatusNotifier recordingValidationStatusNotifier() {
    return new RecordingValidationStatusNotifier();
  }

  /**
   * Guards against the recording beans quietly losing their primary status: the pipeline would then
   * publish to PubSub and the tests would assert nothing.
   */
  @Bean
  PipelineTestConfigCheck pipelineTestConfigCheck(
    JobQueue jobQueue,
    ValidationStatusNotifier validationStatusNotifier
  ) {
    if (
      !(jobQueue instanceof RecordingJobQueue) ||
      !(validationStatusNotifier instanceof RecordingValidationStatusNotifier)
    ) {
      throw new IllegalStateException(
        "The test doubles are not in use: got " +
        jobQueue.getClass() +
        " and " +
        validationStatusNotifier.getClass()
      );
    }
    return new PipelineTestConfigCheck();
  }

  static class PipelineTestConfigCheck {}
}
