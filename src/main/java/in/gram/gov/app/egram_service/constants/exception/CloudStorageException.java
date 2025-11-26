package in.gram.gov.app.egram_service.constants.exception;

/**
 * Exception thrown when cloud storage operations fail
 */
public class CloudStorageException extends RuntimeException {

    public CloudStorageException(String message) {
        super(message);
    }

    public CloudStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public CloudStorageException(Throwable cause) {
        super(cause);
    }
}

