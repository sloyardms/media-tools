package sloyardms.mediatools.image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/** Reads displayed image dimensions from the header and EXIF orientation; pixel data is never decoded. */
public final class ImageInfoReader {

    private ImageInfoReader() {
    }

    public static ImageInfo read(Path file) throws IOException {
        int orientation = ExifOrientation.read(file);
        return ImageReaders.withReader(file, reader -> ImageInfo.displayed(
                file, reader.getWidth(0), reader.getHeight(0), orientation,
                reader.getFormatName().toLowerCase(Locale.ROOT)));
    }
}
