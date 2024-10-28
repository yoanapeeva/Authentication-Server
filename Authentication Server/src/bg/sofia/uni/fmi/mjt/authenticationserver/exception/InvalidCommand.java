package bg.sofia.uni.fmi.mjt.authenticationserver.exception;

public class InvalidCommand extends Exception {
    public InvalidCommand(String message) {
        super(message);
    }

    public InvalidCommand(String message, Throwable cause) {
        super(message, cause);
    }
}
