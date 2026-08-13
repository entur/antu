package no.entur.antu.leader;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import no.entur.antu.config.EmbeddedRedisTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.client.codec.StringCodec;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

class RedisLeaseLeaderElectionTest extends EmbeddedRedisTestBase {

  private static final long LEASE_SECONDS = 30;

  /**
   * The granted event is published on the leadership callback thread, not the heartbeat thread, so the
   * assertions have to wait for it.
   */
  private final List<Object> events = new CopyOnWriteArrayList<>();
  private final ApplicationEventPublisher eventPublisher =
    new ApplicationEventPublisher() {
      @Override
      public void publishEvent(ApplicationEvent event) {
        events.add(event);
      }

      @Override
      public void publishEvent(Object event) {
        events.add(event);
      }
    };

  @BeforeEach
  void clearLease() {
    redissonClient.getBucket("ANTU_LEADER").delete();
    events.clear();
  }

  private RedisLeaseLeaderElection pod() {
    return new RedisLeaseLeaderElection(
      redissonClient,
      eventPublisher,
      LEASE_SECONDS
    );
  }

  @Test
  void aPodIsNotLeaderBeforeItsFirstHeartbeat() {
    assertFalse(pod().isLeader());
  }

  @Test
  void theFirstPodToHeartbeatBecomesLeader() {
    RedisLeaseLeaderElection pod = pod();

    pod.heartbeat();

    assertTrue(pod.isLeader());
    await().atMost(Duration.ofSeconds(5)).until(() -> events.size() == 1);
    assertTrue(events.getFirst() instanceof LeadershipGrantedEvent);
  }

  @Test
  void onlyOnePodIsLeaderAtATime() {
    RedisLeaseLeaderElection first = pod();
    RedisLeaseLeaderElection second = pod();

    first.heartbeat();
    second.heartbeat();

    assertTrue(first.isLeader());
    assertFalse(second.isLeader());
  }

  @Test
  void leadershipIsGrantedOnceNotOnEveryHeartbeat() {
    RedisLeaseLeaderElection pod = pod();

    pod.heartbeat();
    await().atMost(Duration.ofSeconds(5)).until(() -> events.size() == 1);
    pod.heartbeat();
    pod.heartbeat();

    assertTrue(pod.isLeader());
    await()
      .during(Duration.ofMillis(300))
      .atMost(Duration.ofSeconds(5))
      .until(() -> events.size() == 1);
  }

  @Test
  void aHeartbeatExtendsTheLease() {
    RedisLeaseLeaderElection pod = pod();
    pod.heartbeat();

    redissonClient.getBucket("ANTU_LEADER").clearExpire();
    pod.heartbeat();

    long ttl = redissonClient.getBucket("ANTU_LEADER").remainTimeToLive();
    assertTrue(ttl > 0, "the lease must be renewed, got a TTL of " + ttl);
  }

  /**
   * If the lease expired and another pod took over, the old leader has to notice and stand down,
   * otherwise both would run the changelog consumer.
   */
  @Test
  void aPodStandsDownWhenAnotherPodHasTakenTheLease() {
    RedisLeaseLeaderElection first = pod();
    first.heartbeat();
    assertTrue(first.isLeader());

    // Simulate the lease expiring while this pod was stalled, and a second pod claiming it.
    redissonClient.getBucket("ANTU_LEADER").delete();
    RedisLeaseLeaderElection second = pod();
    second.heartbeat();

    first.heartbeat();

    assertFalse(first.isLeader());
    assertTrue(second.isLeader());
  }

  /**
   * A pod that has lost the lease must leave the new owner's expiry alone.
   *
   * <p>This pins the observable contract, not the atomicity. The reason renewal is a single Lua round trip
   * is that a read followed by an expire lets the lease lapse and another pod claim it between the two
   * calls, and the expire then hands that pod an extension it never asked for while this one still believes
   * it leads. That interleaving cannot be reproduced from outside once the two steps are one, so the
   * atomicity rests on the script rather than on this test.
   */
  @Test
  void aPodStandingDownDoesNotExtendTheNewOwnersLease() {
    RedisLeaseLeaderElection first = pod();
    first.heartbeat();
    assertTrue(first.isLeader());

    // Much shorter than LEASE_SECONDS, so an unwanted extension is visible in the TTL.
    RBucket<String> lease = redissonClient.getBucket(
      "ANTU_LEADER",
      StringCodec.INSTANCE
    );
    lease.set("another-pod", Duration.ofSeconds(5));

    first.heartbeat();

    assertFalse(first.isLeader());
    assertEquals(
      "another-pod",
      lease.get(),
      "the other pod must still own the lease"
    );
    long ttl = lease.remainTimeToLive();
    assertTrue(
      ttl <= 5000,
      "the other pod's lease must not have been extended, TTL is " + ttl + " ms"
    );
  }

  /**
   * Taking over means priming the stop place cache, which takes minutes. If that ran on the heartbeat
   * thread the lease would expire underneath it, and the pod would re-elect itself and start priming
   * again, over and over.
   */
  @Test
  void aSlowTakeoverDoesNotCostTheLeaseOrRunTwice() throws Exception {
    CountDownLatch takeoverStarted = new CountDownLatch(1);
    CountDownLatch releaseTakeover = new CountDownLatch(1);
    AtomicInteger takeovers = new AtomicInteger();
    ApplicationEventPublisher blockingPublisher = event -> {
      takeovers.incrementAndGet();
      takeoverStarted.countDown();
      try {
        releaseTakeover.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    };
    RedisLeaseLeaderElection pod = new RedisLeaseLeaderElection(
      redissonClient,
      blockingPublisher,
      LEASE_SECONDS
    );

    pod.heartbeat();

    assertTrue(
      takeoverStarted.await(5, TimeUnit.SECONDS),
      "the takeover should have started"
    );
    // The heartbeat returned while the takeover is still running, so it can keep renewing.
    pod.heartbeat();
    pod.heartbeat();

    assertTrue(pod.isLeader());
    assertEquals(1, takeovers.get());
    releaseTakeover.countDown();
  }

  /**
   * The flag records a decision the heartbeat made up to a heartbeat interval ago. If this pod stalls for
   * longer than the lease, a stop-the-world pause being the usual way, the lease expires in Redis and another
   * pod takes it while this one still believes it leads, and both sweep and both enqueue refreshes. A zero
   * lease stands in for a pause that outlasted it.
   */
  @Test
  void leadershipLapsesWithTheLeaseRatherThanWaitingForTheNextHeartbeat() {
    RedisLeaseLeaderElection pod = new RedisLeaseLeaderElection(
      redissonClient,
      eventPublisher,
      0
    );

    pod.heartbeat();

    assertFalse(
      pod.isLeader(),
      "a lease that has already run out must not still count as leadership"
    );
  }

  @Test
  void aPodCanBecomeLeaderAgainAfterTheLeaseIsFree() {
    RedisLeaseLeaderElection pod = pod();
    pod.heartbeat();

    redissonClient.getBucket("ANTU_LEADER").delete();
    pod.heartbeat();
    assertFalse(pod.isLeader(), "it stands down first");

    pod.heartbeat();
    assertTrue(pod.isLeader(), "and can then take the free lease");
  }
}
