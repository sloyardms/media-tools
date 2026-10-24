package sloyardms.mediatools.menu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Prompting helpers over an input and an output stream, so actions can be driven by scripted input in tests.
 */
public class ConsoleIO {

    private final BufferedReader in;
    private final PrintStream out;

    public ConsoleIO(InputStream in, PrintStream out, Charset inputCharset) {
        this.in = new BufferedReader(new InputStreamReader(in, inputCharset));
        this.out = out;
    }

    public static ConsoleIO system() {
        Charset charset = System.console() != null ? System.console().charset() : Charset.defaultCharset();
        return new ConsoleIO(System.in, System.out, charset);
    }

    public PrintStream out() {
        return out;
    }

    public void println() {
        out.println();
    }

    public void println(String text) {
        out.println(text);
    }

    public void printf(String format, Object... args) {
        out.printf(Locale.ROOT, format, args);
    }

    /** Prints the prompt and returns the trimmed line that was entered. */
    public String ask(String prompt) {
        out.print(prompt);
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                throw new InputClosedException();
            }
            return line.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Like {@link #ask(String)}, returning {@code defaultValue} when the answer is empty. */
    public String ask(String prompt, String defaultValue) {
        String answer = ask(prompt + " [" + defaultValue + "]: ");
        return answer.isEmpty() ? defaultValue : answer;
    }

    public boolean askYesNo(String prompt, boolean defaultValue) {
        String hint = defaultValue ? " [Y/n]: " : " [y/N]: ";
        while (true) {
            String answer = ask(prompt + hint).toLowerCase(Locale.ROOT);
            if (answer.isEmpty()) {
                return defaultValue;
            }
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            println("Please answer y or n.");
        }
    }

    public int askInt(String prompt, int defaultValue, int min, int max) {
        while (true) {
            String answer = ask(prompt, Integer.toString(defaultValue));
            try {
                int value = Integer.parseInt(answer);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            printf("Please enter a whole number between %d and %d.%n", min, max);
        }
    }

    public double askDouble(String prompt, double defaultValue, double min, double max) {
        while (true) {
            String answer = ask(prompt, formatNumber(defaultValue));
            try {
                double value = Double.parseDouble(answer.replace(',', '.'));
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                // fall through to the message below
            }
            printf("Please enter a number between %s and %s.%n", formatNumber(min), formatNumber(max));
        }
    }

    /** Asks until an existing directory is entered. Surrounding quotes (from "Copy as path") are ignored. */
    public Path askExistingDirectory(String prompt) {
        while (true) {
            String answer = stripQuotes(ask(prompt));
            if (answer.isEmpty()) {
                continue;
            }
            try {
                Path path = Path.of(answer).toAbsolutePath().normalize();
                if (Files.isDirectory(path)) {
                    return path;
                }
                println("Not a folder: " + path);
            } catch (InvalidPathException e) {
                println("Invalid path: " + e.getMessage());
            }
        }
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }
}
