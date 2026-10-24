package sloyardms.mediatools.image;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Opens a file with the ImageIO readers that claim it, trying the next reader if one fails.
 * The stream is closed before returning, so the file is not locked afterwards (matters on Windows).
 */
public final class ImageReaders {

    @FunctionalInterface
    public interface ReaderFunction<T> {
        T apply(ImageReader reader) throws IOException;
    }

    private ImageReaders() {
    }

    public static <T> T withReader(Path file, ReaderFunction<T> function) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 0; ; attempt++) {
            try (ImageInputStream stream = ImageIO.createImageInputStream(file.toFile())) {
                if (stream == null) {
                    throw new IOException("Cannot open " + file);
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                for (int skip = 0; skip < attempt && readers.hasNext(); skip++) {
                    readers.next();
                }
                if (!readers.hasNext()) {
                    throw lastFailure != null ? lastFailure : new IOException("Not a supported image: " + file.getFileName());
                }

                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream, true, true);
                    return function.apply(reader);
                } catch (IOException e) {
                    lastFailure = e;
                } catch (RuntimeException e) {
                    // Some decoders throw unchecked exceptions on corrupt data.
                    lastFailure = new IOException("Cannot decode " + file.getFileName() + ": " + e, e);
                } finally {
                    reader.dispose();
                }
            }
        }
    }
}
