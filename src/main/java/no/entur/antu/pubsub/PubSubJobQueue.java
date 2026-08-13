package no.entur.antu.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PubSubJobQueue implements JobQueue {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    PubSubJobQueue.class
  );

  private final PubSubTemplate pubSubTemplate;

  public PubSubJobQueue(PubSubTemplate pubSubTemplate) {
    this.pubSubTemplate = pubSubTemplate;
  }

  @Override
  public void submit(AntuJob job) {
    JobMessageCodec.JobMessage message = JobMessageCodec.encode(job);
    LOGGER.debug("Submitting job {}", job.type());
    PubSubPublishing.publishAndWait(
      pubSubTemplate,
      AntuQueues.JOB_QUEUE,
      message.body(),
      message.attributes()
    );
  }
}
