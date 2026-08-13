package no.entur.antu.leader;

/**
 * Elects one pod to prime the stop place cache and to decide when the caches are refreshed. The refreshes
 * themselves are jobs, and run on whichever pod picks them up.
 */
public interface LeaderElection {
  boolean isLeader();
}
