package no.entur.antu.stop.changelog;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.kafka.ChangelogConsumerController;

class ChangelogStopPlaceRepositoryUpdaterTest {

  private StopPlaceRepositoryLoader stopPlaceRepositoryLoader;
  private ChangelogConsumerController changelogConsumerController;
  private StopPlaceChangelog stopPlaceChangelog;
  private AntuStopPlaceChangeLogListener handler;
  private ChangelogStopPlaceRepositoryUpdater updater;

  @BeforeEach
  void setUp() {
    stopPlaceRepositoryLoader = mock(StopPlaceRepositoryLoader.class);
    changelogConsumerController = mock(ChangelogConsumerController.class);
    stopPlaceChangelog = mock(StopPlaceChangelog.class);
    handler = mock(AntuStopPlaceChangeLogListener.class);
    updater =
      new ChangelogStopPlaceRepositoryUpdater(
        stopPlaceRepositoryLoader,
        mock(AntuPublicationTimeRecordFilterStrategy.class),
        mock(RedisChangelogUpdateTimestampRepository.class),
        changelogConsumerController,
        stopPlaceChangelog,
        handler
      );
  }

  /**
   * A refresh that throws must not leave the consumer stopped with its listener unregistered: it would
   * stay that way until the next refresh or the next leader.
   */
  @Test
  void aFailedRefreshStillLeavesTheConsumerRunning() {
    when(stopPlaceRepositoryLoader.refreshCache())
      .thenThrow(new IllegalStateException("no stop places in the dataset"));

    assertThrows(IllegalStateException.class, () -> updater.createOrUpdate());

    verify(stopPlaceChangelog).registerStopPlaceChangelogListener(handler);
    verify(changelogConsumerController).start();
  }

  /**
   * A refresh stops the consumer and unregisters the listener while it parses the dataset, and a pod
   * taking over as leader initialises the updater on another thread. Registering and starting inside that
   * window would consume changes with no listener registered, and they would not be seen again until the
   * container next restarts. Run this against an updater without the synchronized: init runs to completion
   * instead of blocking, which is the only thing that makes it a test of the fix.
   */
  @Test
  void initWaitsForARefreshToPutTheConsumerBack() throws Exception {
    CountDownLatch refreshStarted = new CountDownLatch(1);
    CountDownLatch letRefreshFinish = new CountDownLatch(1);
    CountDownLatch initReturned = new CountDownLatch(1);
    when(stopPlaceRepositoryLoader.refreshCache())
      .thenAnswer(invocation -> {
        refreshStarted.countDown();
        assertTrue(letRefreshFinish.await(5, TimeUnit.SECONDS));
        return Instant.now();
      });

    Thread refresh = new Thread(updater::createOrUpdate);
    refresh.start();
    assertTrue(refreshStarted.await(5, TimeUnit.SECONDS));

    Thread takeOverAsLeader = new Thread(() -> {
      updater.init();
      initReturned.countDown();
    });
    takeOverAsLeader.start();

    await()
      .atMost(Duration.ofSeconds(5))
      .until(() -> takeOverAsLeader.getState() == Thread.State.BLOCKED);
    assertFalse(initReturned.getCount() == 0);
    verify(changelogConsumerController, never()).start();

    letRefreshFinish.countDown();
    refresh.join(TimeUnit.SECONDS.toMillis(5));
    assertTrue(initReturned.await(5, TimeUnit.SECONDS));
    assertFalse(refresh.isAlive());
    verify(changelogConsumerController, times(2)).start();
  }

  /**
   * A restart failure must not make the refresh look successful: the refresher would keep the recorded
   * version and no check would retry, leaving the changelog dead until the next export.
   */
  @Test
  void aConsumerThatCannotBeRestartedFailsTheRefresh() {
    when(stopPlaceRepositoryLoader.refreshCache()).thenReturn(Instant.now());
    doThrow(new IllegalStateException("kafka is down"))
      .when(changelogConsumerController)
      .start();

    assertThrows(IllegalStateException.class, () -> updater.createOrUpdate());
  }

  /**
   * When both fail the refresh failure is the one that propagates, so the log names the real cause.
   */
  @Test
  void aFailedRefreshOutranksAFailedRestart() {
    doThrow(new IllegalStateException("bad export"))
      .when(stopPlaceRepositoryLoader)
      .refreshCache();
    doThrow(new IllegalStateException("kafka is down"))
      .when(changelogConsumerController)
      .start();

    IllegalStateException thrown = assertThrows(
      IllegalStateException.class,
      () -> updater.createOrUpdate()
    );

    assertEquals("bad export", thrown.getMessage());
    assertEquals("kafka is down", thrown.getSuppressed()[0].getMessage());
  }
}
