package sloyardms.mediatools.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageFilesTest {

    @TempDir
    Path root;

    @Test
    void extensionIsLowerCaseWithoutDot() {
        assertEquals("jpg", ImageFiles.extension(Path.of("Photo.MD.JPG")));
        assertEquals("md", ImageFiles.extension(Path.of("photo.md")));
        assertEquals("", ImageFiles.extension(Path.of("README")));
        assertEquals("", ImageFiles.extension(Path.of(".hidden")));
    }

    @Test
    void allowlistSkipsVideosAndOtherFiles() {
        assertTrue(ImageFiles.hasImageExtension(Path.of("a.webp")));
        assertTrue(ImageFiles.hasImageExtension(Path.of("a.TIFF")));
        assertFalse(ImageFiles.hasImageExtension(Path.of("a.mp4")));
        assertFalse(ImageFiles.hasImageExtension(Path.of("a.mov")));
        assertFalse(ImageFiles.hasImageExtension(Path.of("a.md")));
    }

    @Test
    void listsTopLevelOnlyOrRecursivelyAndSkipsRemovedFolder() throws IOException {
        Files.createDirectories(root.resolve("sub/deeper"));
        Files.createDirectories(root.resolve("_Removed"));
        Path top = Files.createFile(root.resolve("a.jpg"));
        Path nested = Files.createFile(root.resolve("sub/deeper/b.jpg"));
        Files.createFile(root.resolve("_Removed/c.jpg"));

        assertEquals(List.of(top), ImageFiles.listFiles(root, false, Set.of("_removed")));
        assertEquals(List.of(top, nested), ImageFiles.listFiles(root, true, Set.of("_removed")));
    }
}
