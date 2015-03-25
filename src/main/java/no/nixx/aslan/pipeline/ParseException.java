package no.nixx.aslan.pipeline;

public class ParseException extends RuntimeException {

    private static final long serialVersionUID = 5733478303041958967L;

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}