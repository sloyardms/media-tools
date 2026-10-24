package sloyardms.mediatools.thumbnails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import sloyardms.mediatools.image.ImageInfo;

class NameMatcherTest {

    private final NameMatcher matcher = new NameMatcher(ThumbnailOptions.DEFAULT_MARKERS, 6);

    @Test
    void parsesMarkedAndPlainNames() {
        assertParsed("photo.md.jpg", "photo", true);
        assertParsed("Photo.MD.webp", "photo", true);
        assertParsed("photo.md", "photo", true);
        assertParsed("photo.thumb.png", "photo", true);
        assertParsed("photo.jpg", "photo", false);
        assertParsed("my.trip.2024.jpg", "my.trip.2024", false);
        assertParsed("notes.txt", "notes.txt", false);
    }

    @Test
    void namesInDifferentFoldersDoNotMatch() {
        HashedImage original = image("a/photo.jpg", 1000, 750, 0L);
        HashedImage thumbnail = image("b/photo.md.jpg", 200, 150, 0L);

        NameMatcher.Result result = matcher.match(List.of(original, thumbnail));

        assertTrue(result.duplicates().isEmpty());
        assertTrue(result.needsReview().isEmpty());
    }

    @Test
    void twoPlainFilesWithTheSameBaseNameAreNotANameMatch() {
        NameMatcher.Result result = matcher.match(List.of(
                image("photo.jpg", 1000, 750, 0L), image("photo.png", 200, 150, 0L)));
        assertTrue(result.duplicates().isEmpty());
    }

    @Test
    void sameDimensionsGoToReview() {
        NameMatcher.Result result = matcher.match(List.of(
                image("photo.jpg", 200, 150, 0L), image("photo.md.jpg", 200, 150, 0L)));

        assertTrue(result.duplicates().isEmpty());
        assertEquals(1, result.needsReview().size());
        assertEquals("same dimensions", result.needsReview().get(0).reason());
    }

    @Test
    void hashOutsideThresholdGoesToReview() {
        NameMatcher.Result result = matcher.match(List.of(
                image("photo.jpg", 1000, 750, 0L), image("photo.md.jpg", 200, 150, 0xFFL)));

        assertTrue(result.duplicates().isEmpty());
        assertEquals(8, result.needsReview().get(0).distance());
        assertFalse(result.needsReview().get(0).reason().isEmpty());
    }

    private void assertParsed(String name, String base, boolean marked) {
        NameMatcher.NameKey key = matcher.parse(Path.of(name));
        assertEquals(base, key.baseName(), name);
        assertEquals(marked, key.marked(), name);
    }

    private static HashedImage image(String path, int width, int height, long hash) {
        return new HashedImage(new ImageInfo(Path.of(path).toAbsolutePath(), width, height, "jpeg"), hash);
    }
}
