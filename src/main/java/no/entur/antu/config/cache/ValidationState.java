package no.entur.antu.config.cache;

/**
 * The state of an in-progress validation.
 */
public class ValidationState {

  private boolean hasErrorInCommonFile;

  public void setHasErrorInCommonFile(boolean hasErrorInCommonFile) {
    this.hasErrorInCommonFile = hasErrorInCommonFile;
  }

  /**
   * Return true if at least one error or critical validation issue was reported in a common file.
   */
  public boolean hasErrorInCommonFile() {
    return hasErrorInCommonFile;
  }
}
