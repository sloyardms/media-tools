package sloyardms.mediatools.testutil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Random;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/** Generates test pictures: seeded random scenes, resized "thumbnails" and files written in a given format. */
public final class SampleImages {

    private SampleImages() {
    }

    /**
     * A scene of random shapes. Shape positions and sizes are relative to the image size, so the same seed at
     * different sizes is the same picture. Different seeds give unrelated pictures.
     */
    public static BufferedImage scene(long seed, int width, int height) {
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(randomColor(random));
        g.fillRect(0, 0, width, height);
        for (int i = 0; i < 30; i++) {
            g.setColor(randomColor(random));
            int w = (int) (width * (0.05 + random.nextDouble() * 0.35));
            int h = (int) (height * (0.05 + random.nextDouble() * 0.35));
            int x = (int) (width * random.nextDouble()) - w / 2;
            int y = (int) (height * random.nextDouble()) - h / 2;
            if (random.nextBoolean()) {
                g.fillOval(x, y, w, h);
            } else {
                g.fillRect(x, y, w, h);
            }
        }
        g.dispose();
        return image;
    }

    /** Smooth downscale, like a gallery's thumbnail generator would do. */
    public static BufferedImage resize(BufferedImage source, int width, int height) {
        Image scaled = source.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(scaled, 0, 0, null);
        g.dispose();
        return result;
    }

    /** Writes the image in {@code format} ("png", "jpg", "gif", "bmp") regardless of the file name. */
    public static Path write(BufferedImage image, Path file, String format) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            if (!ImageIO.write(image, format, file.toFile())) {
                throw new IllegalStateException("No ImageIO writer for " + format);
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path writeJpeg(BufferedImage image, Path file, float quality) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        ImageWriter writer = writers.next();
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.deleteIfExists(file);
            try (ImageOutputStream out = ImageIO.createImageOutputStream(file.toFile())) {
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
                writer.setOutput(out);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writer.dispose();
        }
    }

    public static Path writeBytes(Path file, byte[] bytes) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            return Files.write(file, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Color randomColor(Random random) {
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
}
