package sloyardms.mediatools.thumbnails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FolderRemoverTest {

    @TempDir
    Path root;

    @Test
    void movesFileKeepingRelativePath() throws IOException {
        Path file = Files.createDirectories(root.resolve("2024/trip")).resolve("photo.md.jpg");
        Files.writeString(file, "x");

        Path moved = new FolderRemover(root, root.resolve("_removed")).remove(file);

        assertEquals(root.resolve("_removed/2024/trip/photo.md.jpg"), moved);
        assertFalse(Files.exists(file));
        assertTrue(Files.exists(moved));
    }

    @Test
    void neverOverwritesAnEarlierRemoval() throws IOException {
        FolderRemover remover = new FolderRemover(root, root.resolve("_removed"));
        Path file = root.resolve("photo.md.jpg");

        Files.writeString(file, "first");
        Path first = remover.remove(file);
        Files.writeString(file, "second");
        Path second = remover.remove(file);

        assertEquals(root.resolve("_removed/photo.md-1.jpg"), second);
        assertEquals("first", Files.readString(first));
        assertEquals("second", Files.readString(second));
    }
}
