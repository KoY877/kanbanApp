package exceptions;

/**
 * Thrown when a requested board cannot be found.
 */
public class  BoardNotFoundException extends RuntimeException {
    /**
     * @param message the error message
     */
    public BoardNotFoundException(String message) {
        super(message);
    }

}
