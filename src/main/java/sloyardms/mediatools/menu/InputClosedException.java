package sloyardms.mediatools.menu;

/** Thrown when standard input reaches end-of-stream while a prompt is waiting for an answer. */
public class InputClosedException extends RuntimeException {

    public InputClosedException() {
        super("Input closed");
    }
}
