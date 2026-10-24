package sloyardms.mediatools.thumbnails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sloyardms.mediatools.thumbnails.DuplicatePair.MatchMethod;

class DuplicateFinderTest {

    @TempDir
    static Path root;

    private static DuplicateFinder.Result recursive;
    private static DuplicateFinder.Result topLevel;

    @BeforeAll
    static void scan() throws IOException {
        GalleryFixture.create(root);
        recursive = new DuplicateFinder(ThumbnailOptions.defaults(root, true)).find(DuplicateFinder.Progress.NONE);
        topLevel = new DuplicateFinder(ThumbnailOptions.defaults(root, false)).find(DuplicateFinder.Progress.NONE);
    }

    @Test
    void findsExactlyTheExpectedDuplicates() {
        Map<String, String> expected = new TreeMap<>(Map.of(
                "sunset.md.jpg", "sunset.png",
                "beach.md", "beach.jpg",
                "forest.thumb.png", "forest.jpg",
                "forest.md.jpg", "forest.jpg",
                "IMG_0042_small.jpg", "mountain.jpg",
                "river.jpg", "river.md.jpg",
                "sub/lake.md.jpg", "sub/lake.jpg"));

        assertEquals(expected, removedToKept(recursive));
    }

    @Test
    void usesNameMatchWhenNamesFitAndHashOtherwise() {
        Map<String, MatchMethod> methods = new TreeMap<>();
        for (DuplicatePair pair : recursive.duplicates()) {
            methods.put(name(pair.removed().path()), pair.method());
        }
        assertEquals(MatchMethod.HASH, methods.get("IMG_0042_small.jpg"));
        assertEquals(MatchMethod.NAME, methods.get("sunset.md.jpg"));
        assertEquals(MatchMethod.NAME, methods.get("beach.md"));
    }

    @Test
    void alwaysKeepsTheImageWithMorePixels() {
        for (DuplicatePair pair : recursive.duplicates()) {
            assertTrue(pair.kept().pixels() > pair.removed().pixels(), pair.toString());
        }
    }

    @Test
    void notesWhenTheKeptFileHasTheThumbnailName() {
        DuplicatePair river = recursive.duplicates().stream()
                .filter(p -> name(p.removed().path()).equals("river.jpg")).findFirst().orElseThrow();
        assertFalse(river.note().isEmpty());
    }

    @Test
    void sameBaseNameWithUnrelatedImagesNeedsReviewAndIsNotRemoved() {
        assertEquals(1, recursive.needsReview().size());
        ReviewPair review = recursive.needsReview().get(0);
        assertEquals("city.jpg", name(review.larger().path()));
        assertEquals("city.md.jpg", name(review.smaller().path()));
        assertTrue(review.distance() > ThumbnailOptions.DEFAULT_HASH_THRESHOLD);

        assertFalse(removedToKept(recursive).containsKey("city.md.jpg"));
        assertFalse(removedToKept(recursive).containsKey("city.jpg"));
    }

    @Test
    void ignoresNonImagesAndReportsBrokenImages() {
        assertEquals(List.of(root.resolve("broken.jpg")), recursive.unreadable());
        // 19 image candidates outside _removed (beach.md included, notes.md is not an image), minus broken.jpg.
        assertEquals(18, recursive.imagesScanned());
    }

    @Test
    void doesNotMatchAcrossFolders() {
        assertFalse(removedToKept(recursive).containsKey("sub/mountain-copy.jpg"));
    }

    @Test
    void topLevelScanLeavesSubfoldersOut() {
        Map<String, String> pairs = removedToKept(topLevel);
        assertEquals(6, pairs.size());
        assertFalse(pairs.containsKey("sub/lake.md.jpg"));
        assertEquals(15, topLevel.imagesScanned());
    }

    @Test
    void stricterThresholdTurnsNameMatchesIntoReview() throws IOException {
        ThumbnailOptions strict = new ThumbnailOptions(root, true, 0, 0.02, ThumbnailOptions.DEFAULT_MARKERS);
        DuplicateFinder.Result result = new DuplicateFinder(strict).find(DuplicateFinder.Progress.NONE);
        // Nothing is ever removed without its hash agreeing.
        for (DuplicatePair pair : result.duplicates()) {
            assertEquals(0, pair.distance());
        }
    }

    private static Map<String, String> removedToKept(DuplicateFinder.Result result) {
        Map<String, String> map = new TreeMap<>();
        for (DuplicatePair pair : result.duplicates()) {
            map.put(name(pair.removed().path()), name(pair.kept().path()));
        }
        return map;
    }

    private static String name(Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }
}
