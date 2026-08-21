package no.entur.antu.stop.changelog;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Test
  void aStoppedConsumerIsStartedAgain() {
    when(changelogConsumerController.isRunning()).thenReturn(false);

    updater.ensureRunning();

    verify(changelogConsumerController).start();
  }

  @Test
  void aRunningConsumerIsLeftAlone() {
    when(changelogConsumerController.isRunning()).thenReturn(true);

    updater.ensureRunning();

    verify(changelogConsumerController, never()).start();
  }

  /**
   * The refresh stops the consumer and unregisters the listener while it parses the dataset. Starting it
   * during that window would consume changes with no listener registered, and they would not be seen
   * again until the container next restarts. Run this against an updater without the lifecycle lock: it
   * fails there, which is the only thing that makes it a test of the fix.
   */
  @Test
  void aRefreshInProgressIsNotMistakenForAStoppedConsumer() throws Exception {
    CountDownLatch refreshStarted = new CountDownLatch(1);
    CountDownLatch letRefreshFinish = new CountDownLatch(1);
    when(stopPlaceRepositoryLoader.refreshCache())
      .thenAnswer(invocation -> {
        refreshStarted.countDown();
        assertTrue(letRefreshFinish.await(5, TimeUnit.SECONDS));
        return Instant.now();
      });
    // What the refresh leaves behind while it runs, having stopped the consumer itself.
    when(changelogConsumerController.isRunning()).thenReturn(false);

    Thread refresh = new Thread(updater::createOrUpdate);
    refresh.start();
    assertTrue(refreshStarted.await(5, TimeUnit.SECONDS));

    updater.ensureRunning();

    verify(changelogConsumerController, never()).start();
    verify(stopPlaceChangelog, never())
      .registerStopPlaceChangelogListener(handler);

    letRefreshFinish.countDown();
    refresh.join(TimeUnit.SECONDS.toMillis(5));
    verify(changelogConsumerController).start();
  }
}
