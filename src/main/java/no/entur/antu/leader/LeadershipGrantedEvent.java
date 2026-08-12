package no.entur.antu.leader;

/**
 * Published on the pod that has just become leader.
 *
 * <p>Carries nothing on purpose: the listeners prime caches, and which pod is leading is already in the
 * election's own log line.
 */
@SuppressWarnings("java:S2094")
public record LeadershipGrantedEvent() {}
