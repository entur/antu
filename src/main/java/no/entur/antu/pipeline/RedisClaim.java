package no.entur.antu.pipeline;

import org.redisson.api.RBucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the work a Redis claim was just taken for, and hands the claim back if that work fails.
 *
 * <p>A claim is taken before an action that must happen for exactly one caller, so that several pods
 * reaching the same conclusion at the same instant produce one action rather than several. If the action
 * then fails and the claim stays taken, the redelivery finds it held, does nothing, and the validation
 * ends with no terminal status at all, which is worse than the duplicate the claim prevents.
 *
 * <p>Lives in one place because there are two claim sites and the rule is easy to half-implement: release
 * on any {@code Throwable}, and never let the release mask the failure that caused it.
 */
final class RedisClaim {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    RedisClaim.class
  );

  private RedisClaim() {}

  /**
   * @param description names the claim in the log, e.g. "the completion claim for report x".
   */
  static void runOrRelease(
    RBucket<Boolean> claim,
    String description,
    Runnable work
  ) {
    boolean succeeded = false;
    try {
      work.run();
      succeeded = true;
    } finally {
      if (!succeeded) {
        release(claim, description);
      }
    }
  }

  private static void release(RBucket<Boolean> claim, String description) {
    try {
      claim.delete();
      LOGGER.warn("Released {} after the work it guards failed", description);
    } catch (Exception e) {
      LOGGER.error(
        "Could not release {}; it will stall until the sweep gives up on it",
        description,
        e
      );
    }
  }
}
