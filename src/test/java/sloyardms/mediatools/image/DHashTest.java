package sloyardms.mediatools.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sloyardms.mediatools.testutil.SampleImages;
import sloyardms.mediatools.thumbnails.ThumbnailOptions;

class DHashTest {

    @TempDir
    Path dir;

    @Test
    void sameImageHasDistanceZero() {
        BufferedImage image = SampleImages.scene(1, 400, 300);
        assertEquals(0, DHash.distance(DHash.compute(image), DHash.compute(image)));
    }

    @Test
    void distanceCountsDifferingBits() {
        assertEquals(0, DHash.distance(0L, 0L));
        assertEquals(64, DHash.distance(0L, -1L));
        assertEquals(2, DHash.distance(0b1011L, 0b0001L));
    }

    @Test
    void largeOriginalAndSmallThumbnailAreWithinThreshold() throws Exception {
        BufferedImage original = SampleImages.scene(42, 4000, 3000);
        Path originalFile = SampleImages.writeJpeg(original, dir.resolve("original.jpg"), 0.92f);
        Path thumbnailFile = SampleImages.writeJpeg(SampleImages.resize(original, 160, 120),
                dir.resolve("original.md.jpg"), 0.7f);

        DHash.Result large = DHash.compute(originalFile);
        DHash.Result small = DHash.compute(thumbnailFile);

        assertEquals(4000, large.info().width());
        assertEquals(120, small.info().height());
        int distance = DHash.distance(large.hash(), small.hash());
        assertTrue(distance <= ThumbnailOptions.DEFAULT_HASH_MATCH_MAX_DISTANCE,
                "original vs thumbnail distance was " + distance);
    }

    @Test
    void unrelatedImagesAreFarApart() {
        int closest = 64;
        for (int seed = 0; seed < 10; seed++) {
            long a = DHash.compute(SampleImages.scene(seed, 400, 300));
            long b = DHash.compute(SampleImages.scene(seed + 100, 400, 300));
            closest = Math.min(closest, DHash.distance(a, b));
        }
        assertTrue(closest > ThumbnailOptions.DEFAULT_NAME_MATCH_MAX_DISTANCE, "closest unrelated distance was " + closest);
    }

    @Test
    void transparentPixelsAreTreatedAsWhite() {
        BufferedImage transparent = new BufferedImage(90, 80, BufferedImage.TYPE_INT_ARGB);
        double[] gray = DHash.areaAverageGray(transparent, 9, 8);
        for (double value : gray) {
            assertEquals(255.0, value, 1e-9);
        }
    }
}
