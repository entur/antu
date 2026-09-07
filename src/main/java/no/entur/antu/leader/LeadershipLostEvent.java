package no.entur.antu.leader;

/**
 * Published on the pod that has just stopped being leader.
 *
 * <p>The counterpart to {@link LeadershipGrantedEvent}: whatever a pod started on taking over has to be
 * stood down again, or it keeps running alongside the new leader's.
 */
@SuppressWarnings("java:S2094")
public record LeadershipLostEvent() {}
