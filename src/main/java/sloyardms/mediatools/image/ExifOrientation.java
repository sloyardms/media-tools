package sloyardms.mediatools.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;

/**
 * EXIF orientation (tag 0x0112, values 1-8): how the stored pixels must be transformed to display the image
 * upright. Applied so that an original with only the flag matches a thumbnail that was saved pre-rotated.
 */
public final class ExifOrientation {

    public static final int NORMAL = 1;

    private ExifOrientation() {
    }

    /** The file's orientation, or {@link #NORMAL} if it has none or the metadata cannot be read. */
    public static int read(Path file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
            for (ExifIFD0Directory directory : metadata.getDirectoriesOfType(ExifIFD0Directory.class)) {
                if (directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                    int orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                    if (orientation >= 1 && orientation <= 8) {
                        return orientation;
                    }
                }
            }
        } catch (ImageProcessingException | IOException | MetadataException | RuntimeException e) {
            // Missing or broken metadata: treat as upright.
        }
        return NORMAL;
    }

    /** Orientations 5-8 rotate by 90 degrees, so the displayed width is the stored height. */
    public static boolean swapsDimensions(int orientation) {
        return orientation >= 5 && orientation <= 8;
    }

    /** Returns the image as it should be displayed. */
    public static BufferedImage apply(BufferedImage image, int orientation) {
        if (orientation <= NORMAL || orientation > 8) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        boolean swap = swapsDimensions(orientation);
        int outWidth = swap ? h : w;
        int outHeight = swap ? w : h;

        int[] src = image.getRGB(0, 0, w, h, null, 0, w);
        int[] dst = new int[src.length];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int dx;
                int dy;
                switch (orientation) {
                    case 2 -> { dx = w - 1 - x; dy = y; }          // mirror horizontal
                    case 3 -> { dx = w - 1 - x; dy = h - 1 - y; }  // rotate 180
                    case 4 -> { dx = x; dy = h - 1 - y; }          // mirror vertical
                    case 5 -> { dx = y; dy = x; }                  // transpose
                    case 6 -> { dx = h - 1 - y; dy = x; }          // rotate 90 clockwise
                    case 7 -> { dx = h - 1 - y; dy = w - 1 - x; }  // transverse
                    default -> { dx = y; dy = w - 1 - x; }         // 8: rotate 90 counter-clockwise
                }
                dst[dy * outWidth + dx] = src[y * w + x];
            }
        }

        BufferedImage result = new BufferedImage(outWidth, outHeight, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, outWidth, outHeight, dst, 0, outWidth);
        return result;
    }
}
