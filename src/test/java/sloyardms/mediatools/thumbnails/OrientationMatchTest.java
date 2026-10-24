package sloyardms.mediatools.thumbnails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sloyardms.mediatools.image.DHash;
import sloyardms.mediatools.image.ExifOrientation;
import sloyardms.mediatools.testutil.SampleImages;

/**
 * A camera stores a portrait photo as landscape pixels plus an orientation flag; the gallery's thumbnail
 * generator saved the thumbnail already rotated, without the flag.
 */
class OrientationMatchTest {

    @TempDir
    Path root;

    @Test
    void preRotatedThumbnailMatchesOriginalWithOnlyTheOrientationFlag() throws IOException {
        // Displayed pictures are portrait. Stored pixels are rotated back 90 degrees counter-clockwise, which
        // orientation 6 (rotate 90 clockwise to display) undoes.
        BufferedImage portrait = SampleImages.scene(21, 900, 1200);
        BufferedImage stored = ExifOrientation.apply(portrait, 8);
        Path original = SampleImages.writeJpeg(stored, root.resolve("portrait.jpg"), 0.9f);
        SampleImages.addExifOrientation(original, 6);
        SampleImages.writeJpeg(SampleImages.resize(portrait, 150, 200), root.resolve("portrait.md.jpg"), 0.8f);

        BufferedImage other = SampleImages.scene(22, 900, 1200);
        Path otherOriginal = SampleImages.writeJpeg(ExifOrientation.apply(other, 8), root.resolve("IMG_1234.jpg"), 0.9f);
        SampleImages.addExifOrientation(otherOriginal, 6);
        SampleImages.writeJpeg(SampleImages.resize(other, 150, 200), root.resolve("small-copy.jpg"), 0.8f);

        // Without applying the flag the original would not match its thumbnail at all.
        long thumbnailHash = DHash.compute(root.resolve("portrait.md.jpg")).hash();
        assertTrue(DHash.distance(DHash.compute(stored), thumbnailHash) > ThumbnailOptions.DEFAULT_NAME_MATCH_MAX_DISTANCE);

        DuplicateFinder.Result result = new DuplicateFinder(ThumbnailOptions.defaults(root, false))
                .find(DuplicateFinder.Progress.NONE);

        assertTrue(result.needsReview().isEmpty(), result.needsReview().toString());
        List<DuplicatePair> pairs = result.duplicates();
        assertEquals(2, pairs.size(), pairs.toString());

        DuplicatePair byName = pairs.stream().filter(p -> p.method() == DuplicatePair.MatchMethod.NAME).findFirst().orElseThrow();
        assertEquals(root.resolve("portrait.jpg"), byName.kept().path());
        assertEquals("900x1200", byName.kept().dimensions());
        assertEquals(root.resolve("portrait.md.jpg"), byName.removed().path());

        DuplicatePair byHash = pairs.stream().filter(p -> p.method() == DuplicatePair.MatchMethod.HASH).findFirst().orElseThrow();
        assertEquals(root.resolve("IMG_1234.jpg"), byHash.kept().path());
        assertEquals(root.resolve("small-copy.jpg"), byHash.removed().path());
    }
}
