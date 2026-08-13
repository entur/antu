package no.entur.antu.job;

/**
 * Hands a job over for asynchronous execution, possibly on another pod.
 */
public interface JobQueue {
  void submit(AntuJob job);
}
