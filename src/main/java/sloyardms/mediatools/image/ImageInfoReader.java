package sloyardms.mediatools.image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Reads image dimensions from the header only; pixel data is never decoded. */
public final class ImageInfoReader {

    private ImageInfoReader() {
    }

    public static ImageInfo read(Path file) throws IOException {
        return ImageReaders.withReader(file, reader -> new ImageInfo(
                file, reader.getWidth(0), reader.getHeight(0), reader.getFormatName().toLowerCase(Locale.ROOT)));
    }
}
