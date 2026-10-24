package sloyardms.mediatools.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;

/**
 * Difference hash (dHash): the image is reduced to 9x8 grayscale and each bit records whether a pixel is
 * brighter than its right-hand neighbour, giving 64 bits.
 *
 * <p>Decoding uses source subsampling only down to about {@value #DECODE_TARGET} px on the long side. Going
 * straight to 9x8 by subsampling would be point sampling, which aliases and makes an original and its
 * thumbnail hash differently. The final step to 9x8 averages every source pixel by its covered area.
 */
public final class DHash {

    /** Approximate long-side size that images are decoded at before the smooth reduction. */
    public static final int DECODE_TARGET = 256;

    private static final int HASH_WIDTH = 9;
    private static final int HASH_HEIGHT = 8;

    private DHash() {
    }

    /** Result of hashing a file, with the displayed dimensions taken from its header and EXIF orientation. */
    public record Result(ImageInfo info, long hash) {
    }

    /** Hashes the image as displayed, i.e. after applying its EXIF orientation. */
    public static Result compute(Path file) throws IOException {
        int orientation = ExifOrientation.read(file);
        return ImageReaders.withReader(file, reader -> {
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            ImageInfo info = ImageInfo.displayed(file, width, height, orientation,
                    reader.getFormatName().toLowerCase(Locale.ROOT));
            BufferedImage upright = ExifOrientation.apply(decodeReduced(reader, width, height), orientation);
            return new Result(info, compute(upright));
        });
    }

    public static long compute(BufferedImage image) {
        double[] gray = areaAverageGray(image, HASH_WIDTH, HASH_HEIGHT);
        long hash = 0;
        for (int y = 0; y < HASH_HEIGHT; y++) {
            for (int x = 0; x < HASH_WIDTH - 1; x++) {
                hash <<= 1;
                if (gray[y * HASH_WIDTH + x] > gray[y * HASH_WIDTH + x + 1]) {
                    hash |= 1;
                }
            }
        }
        return hash;
    }

    /** Number of differing bits, from 0 (same) to 64. */
    public static int distance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    private static BufferedImage decodeReduced(ImageReader reader, int width, int height) throws IOException {
        int factor = Math.max(1, Math.max(width, height) / DECODE_TARGET);
        ImageReadParam param = reader.getDefaultReadParam();
        if (factor > 1) {
            param.setSourceSubsampling(factor, factor, 0, 0);
        }
        return reader.read(0, param);
    }

    /**
     * Shrinks the image to {@code targetWidth x targetHeight} luminance values, weighting each source pixel by
     * how much of the target cell it covers. Transparent pixels are composited over white.
     */
    static double[] areaAverageGray(BufferedImage image, int targetWidth, int targetHeight) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);

        double[] luminance = new double[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            double alpha = ((p >>> 24) & 0xFF) / 255.0;
            double lum = 0.299 * ((p >> 16) & 0xFF) + 0.587 * ((p >> 8) & 0xFF) + 0.114 * (p & 0xFF);
            luminance[i] = alpha * lum + (1 - alpha) * 255;
        }

        double cellWidth = (double) width / targetWidth;
        double cellHeight = (double) height / targetHeight;
        double[] result = new double[targetWidth * targetHeight];

        for (int ty = 0; ty < targetHeight; ty++) {
            double y0 = ty * cellHeight;
            double y1 = y0 + cellHeight;
            for (int tx = 0; tx < targetWidth; tx++) {
                double x0 = tx * cellWidth;
                double x1 = x0 + cellWidth;

                double sum = 0;
                double weightSum = 0;
                for (int y = (int) y0; y < Math.min(height, Math.ceil(y1)); y++) {
                    double wy = Math.min(y + 1, y1) - Math.max(y, y0);
                    for (int x = (int) x0; x < Math.min(width, Math.ceil(x1)); x++) {
                        double weight = wy * (Math.min(x + 1, x1) - Math.max(x, x0));
                        sum += luminance[y * width + x] * weight;
                        weightSum += weight;
                    }
                }
                result[ty * targetWidth + tx] = sum / weightSum;
            }
        }
        return result;
    }
}
