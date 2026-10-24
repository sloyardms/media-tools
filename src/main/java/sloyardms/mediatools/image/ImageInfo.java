package sloyardms.mediatools.image;

import java.nio.file.Path;

/** Image dimensions as displayed: read from the file header, with width and height swapped for rotated EXIF. */
public record ImageInfo(Path path, int width, int height, String format) {

    /** Builds the info from stored (header) dimensions and the EXIF orientation. */
    public static ImageInfo displayed(Path path, int storedWidth, int storedHeight, int orientation, String format) {
        return ExifOrientation.swapsDimensions(orientation)
                ? new ImageInfo(path, storedHeight, storedWidth, format)
                : new ImageInfo(path, storedWidth, storedHeight, format);
    }

    public long pixels() {
        return (long) width * height;
    }

    public double aspectRatio() {
        return (double) width / height;
    }

    public String dimensions() {
        return width + "x" + height;
    }
}
