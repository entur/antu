package no.entur.antu.exception;

/**
 * Exception thrown when a file is validated multiple times due to message redelivery.
 */
public class FileAlreadyValidatedException extends AntuException {
    public FileAlreadyValidatedException(String message) {
        super(message);
    }
}
