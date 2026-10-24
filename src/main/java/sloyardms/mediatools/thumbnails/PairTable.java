package sloyardms.mediatools.thumbnails;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import sloyardms.mediatools.menu.ConsoleIO;

/** Prints the dry-run tables. Paths are shown relative to the scanned root. */
final class PairTable {

    private static final int MAX_PATH_WIDTH = 60;

    private PairTable() {
    }

    static void print(ConsoleIO io, Path root, DuplicateFinder.Result result) {
        io.println();
        io.printf("Duplicates to remove (%d):%n", result.duplicates().size());
        if (result.duplicates().isEmpty()) {
            io.println("  (none)");
        } else {
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[] {"#", "Kept", "Size", "Removed", "Size", "Method", "Dist", "Note"});
            int n = 1;
            for (DuplicatePair p : result.duplicates()) {
                rows.add(new String[] {Integer.toString(n++), relative(root, p.kept().path()), p.kept().dimensions(),
                        relative(root, p.removed().path()), p.removed().dimensions(), p.method().name(),
                        Integer.toString(p.distance()), p.note()});
            }
            printRows(io, rows);
        }

        if (!result.needsReview().isEmpty()) {
            io.println();
            io.printf("Needs review (name match, images differ) (%d) - these are NOT removed:%n",
                    result.needsReview().size());
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[] {"#", "Larger", "Size", "Smaller", "Size", "Dist", "Reason"});
            int n = 1;
            for (ReviewPair p : result.needsReview()) {
                rows.add(new String[] {Integer.toString(n++), relative(root, p.larger().path()), p.larger().dimensions(),
                        relative(root, p.smaller().path()), p.smaller().dimensions(),
                        Integer.toString(p.distance()), p.reason()});
            }
            printRows(io, rows);
        }

        if (!result.unreadable().isEmpty()) {
            io.println();
            io.printf("Unreadable images, skipped (%d):%n", result.unreadable().size());
            for (Path path : result.unreadable()) {
                io.println("  " + relative(root, path));
            }
        }
    }

    private static void printRows(ConsoleIO io, List<String[]> rows) {
        int columns = rows.get(0).length;
        int[] widths = new int[columns];
        for (String[] row : rows) {
            for (int c = 0; c < columns; c++) {
                widths[c] = Math.max(widths[c], row[c].length());
            }
        }
        for (int r = 0; r < rows.size(); r++) {
            StringBuilder line = new StringBuilder("  ");
            for (int c = 0; c < columns; c++) {
                String cell = rows.get(r)[c];
                line.append(c == 0 ? padLeft(cell, widths[c]) : padRight(cell, widths[c]));
                if (c < columns - 1) {
                    line.append("  ");
                }
            }
            io.println(line.toString().stripTrailing());
            if (r == 0) {
                StringBuilder rule = new StringBuilder("  ");
                for (int c = 0; c < columns; c++) {
                    rule.append("-".repeat(widths[c])).append(c < columns - 1 ? "  " : "");
                }
                io.println(rule.toString());
            }
        }
    }

    static String relative(Path root, Path path) {
        String text = path.startsWith(root) ? root.relativize(path).toString() : path.toString();
        return text.length() > MAX_PATH_WIDTH ? "..." + text.substring(text.length() - MAX_PATH_WIDTH + 3) : text;
    }

    private static String padRight(String s, int width) {
        return s + " ".repeat(width - s.length());
    }

    private static String padLeft(String s, int width) {
        return " ".repeat(width - s.length()) + s;
    }
}
