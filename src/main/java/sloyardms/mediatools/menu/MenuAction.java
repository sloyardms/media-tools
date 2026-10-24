package sloyardms.mediatools.menu;

/**
 * One option in the main menu. Implement this and register the instance in {@code App} to add a new option.
 */
public interface MenuAction {

    /** Text shown in the menu list. */
    String label();

    /** Runs the option. Any exception is reported and the menu is shown again. */
    void run(ConsoleIO io) throws Exception;
}
