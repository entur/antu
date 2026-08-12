package no.entur.antu.pipeline;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobDispatcher;
import no.entur.antu.job.JobQueue;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Runs jobs in the calling thread instead of putting them on PubSub.
 *
 * <p>A validation is a chain of jobs, so driving that chain here makes a whole dataset validation a
 * single synchronous call: no emulator, no polling for a result, and a stack trace that points at the
 * step that failed. Jobs are queued and drained in submission order rather than executed inline, so
 * one step finishes before the next starts, exactly as it would with a single job queue consumer.
 */
public class RecordingJobQueue implements JobQueue {

  /**
   * The dispatcher depends, through the pipeline steps, on the queue itself, so it is resolved on
   * first use rather than injected.
   */
  private final ObjectProvider<JobDispatcher> jobDispatcher;

  private final Deque<AntuJob> pending = new ArrayDeque<>();
  private final List<AntuJob> submitted = new ArrayList<>();
  private boolean draining;

  public RecordingJobQueue(ObjectProvider<JobDispatcher> jobDispatcher) {
    this.jobDispatcher = jobDispatcher;
  }

  @Override
  public void submit(AntuJob job) {
    submitted.add(job);
    pending.addLast(job);
    if (draining) {
      return;
    }
    draining = true;
    try {
      while (!pending.isEmpty()) {
        jobDispatcher.getObject().dispatch(pending.pollFirst());
      }
    } finally {
      draining = false;
      pending.clear();
    }
  }

  public List<AntuJob> submittedJobs() {
    return List.copyOf(submitted);
  }

  public void reset() {
    submitted.clear();
    pending.clear();
    draining = false;
  }
}
