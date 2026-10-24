package sloyardms.mediatools.thumbnails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sloyardms.mediatools.menu.ConsoleIO;

class RemoveThumbnailDuplicatesActionTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-23T10:15:30Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private RemoveThumbnailDuplicatesAction action;

    @BeforeEach
    void setUp() {
        GalleryFixture.create(root);
        action = new RemoveThumbnailDuplicatesAction(
                r -> new FolderRemover(r, r.resolve(ThumbnailOptions.REMOVED_DIR_NAME)), CLOCK);
    }

    @Test
    void dryRunPrintsTablesAndChangesNothingWhenDeclined() throws IOException {
        List<Path> before = listAll();

        run(root + "\n\n\n\nn\n");

        assertEquals(before, listAll());
        String text = output.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Duplicates to remove (7)"), text);
        assertTrue(text.contains("Needs review (name match, images differ) (1)"), text);
        assertTrue(text.contains("Cancelled"), text);
    }

    @Test
    void confirmedRunMovesDuplicatesSkipsReviewAndWritesLog() throws IOException {
        run("\"" + root + "\"\ny\n\n\ny\n");

        assertFalse(Files.exists(root.resolve("sunset.md.jpg")));
        assertTrue(Files.exists(root.resolve("_removed/sunset.md.jpg")));
        assertTrue(Files.exists(root.resolve("_removed/sub/lake.md.jpg")));
        assertTrue(Files.exists(root.resolve("sunset.png")));
        // Needs-review pair is left alone.
        assertTrue(Files.exists(root.resolve("city.jpg")));
        assertTrue(Files.exists(root.resolve("city.md.jpg")));

        Path log = root.resolve("thumbnail-cleanup-20261023-101530.csv");
        List<String> lines = Files.readAllLines(log, StandardCharsets.UTF_8);
        assertEquals(1 + 7 + 1, lines.size());
        assertEquals(7, lines.stream().filter(l -> l.contains(",MOVED,")).count());
        assertEquals(1, lines.stream().filter(l -> l.contains(",NEEDS_REVIEW,")).count());
    }

    @Test
    void nothingToRemoveSkipsConfirmation() throws IOException {
        Path empty = Files.createDirectories(root.resolve("empty"));
        run(empty + "\nn\n\n\n");
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("Nothing to remove."));
    }

    private void run(String input) throws IOException {
        ConsoleIO io = new ConsoleIO(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        action.run(io);
    }

    private List<Path> listAll() throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.sorted().toList();
        }
    }
}
