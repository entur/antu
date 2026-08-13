package no.entur.antu.pubsub;

/**
 * The PubSub destinations antu uses. Topic and subscription share a name.
 */
public final class AntuQueues {

  /**
   * Validation requests from marduk and kakka.
   */
  public static final String NETEX_VALIDATION_QUEUE =
    "AntuNetexValidationQueue";

  /**
   * Validation status notifications back to marduk and kakka.
   */
  public static final String NETEX_VALIDATION_STATUS_QUEUE =
    "AntuNetexValidationStatusQueue";

  /**
   * Antu's internal work queue.
   */
  public static final String JOB_QUEUE = "AntuJobQueue";

  private AntuQueues() {}
}
