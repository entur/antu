package no.entur.antu.exception;

/**
 * Exception caused by a temporary runtime failure. The task can be retried.
 */
public class RetryableAntuException extends AntuException {
    public RetryableAntuException(Throwable t) {
        super(t);
    }
}
