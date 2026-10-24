package sloyardms.mediatools.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class MenuTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final List<String> calls = new ArrayList<>();

    @Test
    void dispatchesToTheChosenActionUntilExit() {
        Menu menu = new Menu("Test").register(recording("First")).register(recording("Second"));

        menu.run(io("2\n1\n2\n0\n"));

        assertEquals(List.of("Second", "First", "Second"), calls);
        assertTrue(text().contains("1) First"));
        assertTrue(text().contains("0) Exit"));
    }

    @Test
    void reportsUnknownOptionsAndKeepsRunning() {
        Menu menu = new Menu("Test").register(recording("Only"));

        menu.run(io("7\nabc\n1\n0\n"));

        assertEquals(List.of("Only"), calls);
        assertTrue(text().contains("Unknown option: 7"));
        assertTrue(text().contains("Unknown option: abc"));
    }

    @Test
    void actionErrorsAreShownAndTheMenuContinues() {
        MenuAction failing = new MenuAction() {
            @Override
            public String label() {
                return "Broken";
            }

            @Override
            public void run(ConsoleIO io) {
                throw new IllegalStateException("boom");
            }
        };
        Menu menu = new Menu("Test").register(failing).register(recording("Fine"));

        menu.run(io("1\n2\n0\n"));

        assertTrue(text().contains("Error: boom"));
        assertEquals(List.of("Fine"), calls);
    }

    @Test
    void endOfInputExits() {
        new Menu("Test").register(recording("Only")).run(io("1\n"));
        assertEquals(List.of("Only"), calls);
    }

    @Test
    void consoleIoAppliesDefaultsAndRetriesBadInput() {
        ConsoleIO io = io("\nmaybe\ny\nxx\n99\n12\n");
        assertTrue(io.askYesNo("ok?", true));
        assertTrue(io.askYesNo("ok?", false));
        assertEquals(12, io.askInt("n", 6, 0, 64));
    }

    private MenuAction recording(String label) {
        return new MenuAction() {
            @Override
            public String label() {
                return label;
            }

            @Override
            public void run(ConsoleIO io) {
                calls.add(label);
            }
        };
    }

    private ConsoleIO io(String input) {
        return new ConsoleIO(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    private String text() {
        return output.toString(StandardCharsets.UTF_8);
    }
}
