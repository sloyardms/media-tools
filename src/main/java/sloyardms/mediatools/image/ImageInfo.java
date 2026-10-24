package sloyardms.mediatools.image;

import java.nio.file.Path;

/** Image dimensions read from the file header. */
public record ImageInfo(Path path, int width, int height, String format) {

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
