package no.entur.antu.leader;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Leader election on a Redis lease.
 *
 * <p>A pod becomes leader by writing its own id under a key that does not exist yet, and stays leader
 * by rewriting it before the lease runs out. If the pod dies the key expires and another pod takes
 * over within one lease period.
 *
 * <p>Redis is already on the critical path for every validation, so making leadership depend on it adds
 * nothing to the set of things that have to be up.
 */
@Component
public class RedisLeaseLeaderElection implements LeaderElection {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    RedisLeaseLeaderElection.class
  );

  private static final String LEADER_KEY = "ANTU_LEADER";

  /**
   * Extends the lease only while this pod still owns it, in one step. Splitting it into a read and an
   * expire lets the lease lapse and another pod take it in between, and the expire then extends the new
   * owner's lease while this pod goes on believing it leads.
   */
  private static final String RENEW_IF_OWNED =
    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end";

  private final String podId = UUID.randomUUID().toString();
  private final RedissonClient redissonClient;

  /**
   * Plain strings rather than the client's Kryo codec, so the value is what the script above compares
   * against and what {@code redis-cli get ANTU_LEADER} shows.
   */
  private final RBucket<String> leaderLease;
  private final Duration leaseDuration;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * The granted event is handed to this thread rather than published inline.
   *
   * <p>Taking over means priming the stop place cache, which parses the national stop dataset and
   * takes minutes. Doing that on the scheduler thread stops the heartbeat, the lease expires, and the
   * pod re-elects itself and starts priming all over again.
   */
  private final ExecutorService leadershipCallbacks =
    Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "antu-leadership");
      thread.setDaemon(true);
      return thread;
    });

  private volatile boolean leader;

  /**
   * When the lease this pod last took or renewed runs out, on the local clock.
   *
   * <p>{@code leader} on its own is a decision the heartbeat made up to a heartbeat interval ago. If this pod
   * stalls for longer than the lease, a stop-the-world pause being the usual way, the lease expires in Redis
   * and another pod takes it while this one still reads {@code leader} as true. Both would then sweep
   * validations and enqueue cache refreshes.
   */
  private volatile long leaseExpiresAtNanos;

  public RedisLeaseLeaderElection(
    RedissonClient redissonClient,
    ApplicationEventPublisher eventPublisher,
    @Value("${antu.leader.lease.seconds:30}") long leaseSeconds
  ) {
    this.redissonClient = redissonClient;
    this.leaderLease =
      redissonClient.getBucket(LEADER_KEY, StringCodec.INSTANCE);
    this.leaseDuration = Duration.ofSeconds(leaseSeconds);
    this.eventPublisher = eventPublisher;
  }

  @PreDestroy
  void stopLeadershipCallbacks() {
    leadershipCallbacks.shutdownNow();
  }

  /**
   * Deliberately conservative: it can say no while this pod does still hold the lease, just before a renewal
   * lands. Skipping a sweep or a refresh trigger for one heartbeat costs nothing, whereas two pods believing
   * they lead at once produces duplicate terminal statuses.
   */
  @Override
  public boolean isLeader() {
    return leader && System.nanoTime() - leaseExpiresAtNanos < 0;
  }

  /**
   * Runs at a third of the lease so two consecutive Redis hiccups do not cost leadership.
   */
  @Scheduled(
    fixedDelayString = "${antu.leader.heartbeat.millis:10000}",
    initialDelayString = "${antu.leader.heartbeat.initial.millis:2000}"
  )
  void heartbeat() {
    try {
      if (leader) {
        renew();
      } else {
        acquire();
      }
    } catch (Exception e) {
      // A lost Redis connection must not leave a stale leader believing it still owns the lease.
      LOGGER.warn("Leader election heartbeat failed: {}", e.getMessage());
      leader = false;
    }
  }

  private void acquire() {
    if (leaderLease.setIfAbsent(podId, leaseDuration)) {
      // Before the flag, so isLeader() never sees leadership without a deadline to check it against.
      leaseExpiresAtNanos = System.nanoTime() + leaseDuration.toNanos();
      leader = true;
      LOGGER.info("Leadership acquired by {}", podId);
      leadershipCallbacks.execute(() -> {
        try {
          eventPublisher.publishEvent(new LeadershipGrantedEvent());
        } catch (Exception e) {
          LOGGER.error("Taking over as leader failed", e);
        }
      });
    }
  }

  private void renew() {
    // Read before the call, not after: the lease started running down when Redis applied the expiry, so
    // timing it from the reply would credit this pod with however long the round trip took.
    long renewedUntil = System.nanoTime() + leaseDuration.toNanos();
    Long renewed = redissonClient
      .getScript(StringCodec.INSTANCE)
      .eval(
        RScript.Mode.READ_WRITE,
        RENEW_IF_OWNED,
        RScript.ReturnType.LONG,
        List.of(LEADER_KEY),
        podId,
        Long.toString(leaseDuration.toMillis())
      );
    if (renewed != null && renewed == 1L) {
      leaseExpiresAtNanos = renewedUntil;
      return;
    }
    // The lease is gone or belongs to someone else now. Stepping aside is what keeps the single-leader
    // guarantee.
    leader = false;
    LOGGER.warn("Leadership lost by {}", podId);
  }
}
