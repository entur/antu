package no.entur.antu.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import no.entur.antu.validation.state.ValidationStateRepository;
import org.junit.jupiter.api.Test;

class AntuNetexValidationProgressCallbackTest {

  /**
   * {@code StalledValidationSweeper} gives up on a validation whose last recorded progress is old enough.
   * Job dispatch is the only other thing that records progress, so without this a single validator running
   * longer than the stall threshold would be concluded as timed out while it was still working.
   */
  @Test
  void progressKeepsALongValidationFromLookingStalled() {
    ValidationStateRepository validationStateRepository = mock(
      ValidationStateRepository.class
    );

    new AntuNetexValidationProgressCallback(
      validationStateRepository,
      "rb_tst_20260811103000000000"
    )
      .notifyProgress("validating something slow");

    verify(validationStateRepository)
      .recordProgress("rb_tst_20260811103000000000");
  }
}
