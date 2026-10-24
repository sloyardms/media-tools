package sloyardms.mediatools.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sloyardms.mediatools.testutil.SampleImages;

class ImageInfoReaderTest {

    // 1x1 WebP images (lossless and lossy). TwelveMonkeys only reads WebP, so samples cannot be generated.
    private static final String WEBP_LOSSLESS = "UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==";
    private static final String WEBP_LOSSY = "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA";

    @TempDir
    Path dir;

    @ParameterizedTest
    @ValueSource(strings = {"png", "jpg", "gif", "bmp"})
    void readsDimensionsFromHeader(String format) throws IOException {
        Path file = SampleImages.write(SampleImages.scene(1, 321, 123), dir.resolve("image." + format), format);

        ImageInfo info = ImageInfoReader.read(file);

        assertEquals(321, info.width());
        assertEquals(123, info.height());
        assertEquals(321L * 123, info.pixels());
    }

    @ParameterizedTest
    @ValueSource(strings = {WEBP_LOSSLESS, WEBP_LOSSY})
    void readsAndHashesWebp(String base64) throws IOException {
        Path file = SampleImages.writeBytes(dir.resolve("tiny.webp"), Base64.getDecoder().decode(base64));

        ImageInfo info = ImageInfoReader.read(file);
        DHash.Result hashed = DHash.compute(file);

        assertEquals("1x1", info.dimensions());
        assertEquals("1x1", hashed.info().dimensions());
    }

    @Test
    void contentDecidesTheFormatNotTheExtension() throws IOException {
        Path file = SampleImages.write(SampleImages.scene(1, 64, 48), dir.resolve("photo.md"), "jpg");
        assertEquals("64x48", ImageInfoReader.read(file).dimensions());
    }

    @Test
    void nonImageFailsWithIOException() {
        Path file = SampleImages.writeBytes(dir.resolve("broken.jpg"), "not an image".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> ImageInfoReader.read(file));
        assertThrows(IOException.class, () -> DHash.compute(file));
    }
}
