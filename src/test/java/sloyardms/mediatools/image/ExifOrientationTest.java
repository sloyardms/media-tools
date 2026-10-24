package sloyardms.mediatools.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sloyardms.mediatools.testutil.SampleImages;

class ExifOrientationTest {

    @TempDir
    Path dir;

    @Test
    void readsOrientationFromJpegExif() {
        Path flagged = SampleImages.writeJpeg(SampleImages.scene(1, 120, 90), dir.resolve("flagged.jpg"), 0.9f);
        SampleImages.addExifOrientation(flagged, 6);
        Path plain = SampleImages.writeJpeg(SampleImages.scene(1, 120, 90), dir.resolve("plain.jpg"), 0.9f);

        assertEquals(6, ExifOrientation.read(flagged));
        assertEquals(ExifOrientation.NORMAL, ExifOrientation.read(plain));
    }

    @Test
    void rotatedOrientationSwapsReportedDimensions() throws IOException {
        Path file = SampleImages.writeJpeg(SampleImages.scene(1, 120, 90), dir.resolve("photo.jpg"), 0.9f);
        SampleImages.addExifOrientation(file, 8);

        assertEquals("90x120", ImageInfoReader.read(file).dimensions());
        assertEquals("90x120", DHash.compute(file).info().dimensions());
    }

    @Test
    void appliesEachOrientationToPixels() {
        // Stored 2x1 image: A B. Expected displayed layouts listed row by row.
        int a = 0xFFFF0000;
        int b = 0xFF0000FF;
        BufferedImage stored = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        stored.setRGB(0, 0, a);
        stored.setRGB(1, 0, b);

        assertPixels(ExifOrientation.apply(stored, 1), 2, a, b);
        assertPixels(ExifOrientation.apply(stored, 2), 2, b, a);
        assertPixels(ExifOrientation.apply(stored, 3), 2, b, a);
        assertPixels(ExifOrientation.apply(stored, 4), 2, a, b);
        assertPixels(ExifOrientation.apply(stored, 5), 1, a, b);
        assertPixels(ExifOrientation.apply(stored, 6), 1, a, b);
        assertPixels(ExifOrientation.apply(stored, 7), 1, b, a);
        assertPixels(ExifOrientation.apply(stored, 8), 1, b, a);
    }

    private static void assertPixels(BufferedImage image, int width, int... expected) {
        assertEquals(width, image.getWidth());
        int[] actual = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual));
    }
}
