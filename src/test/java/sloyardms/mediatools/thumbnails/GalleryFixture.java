package sloyardms.mediatools.thumbnails;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import sloyardms.mediatools.testutil.SampleImages;

/**
 * A sample gallery folder covering the cases the tool must handle. Seeds identify the picture: files generated
 * from the same seed are the same picture at different sizes.
 */
final class GalleryFixture {

    private GalleryFixture() {
    }

    static void create(Path root) {
        // Name match with different extensions.
        BufferedImage sunset = SampleImages.scene(1, 1200, 900);
        SampleImages.write(sunset, root.resolve("sunset.png"), "png");
        SampleImages.writeJpeg(SampleImages.resize(sunset, 300, 225), root.resolve("sunset.md.jpg"), 0.8f);

        // Name match where the thumbnail has no image extension at all.
        BufferedImage beach = SampleImages.scene(2, 1000, 750);
        SampleImages.writeJpeg(beach, root.resolve("beach.jpg"), 0.9f);
        SampleImages.writeJpeg(SampleImages.resize(beach, 240, 180), root.resolve("beach.md"), 0.8f);

        // One original with two thumbnails of different sizes.
        BufferedImage forest = SampleImages.scene(3, 1200, 800);
        SampleImages.writeJpeg(forest, root.resolve("forest.jpg"), 0.9f);
        SampleImages.write(SampleImages.resize(forest, 150, 100), root.resolve("forest.thumb.png"), "png");
        SampleImages.writeJpeg(SampleImages.resize(forest, 450, 300), root.resolve("forest.md.jpg"), 0.8f);

        // Hash-only match: the thumbnail has an unrelated name.
        BufferedImage mountain = SampleImages.scene(4, 1600, 1200);
        SampleImages.writeJpeg(mountain, root.resolve("mountain.jpg"), 0.9f);
        SampleImages.writeJpeg(SampleImages.resize(mountain, 200, 150), root.resolve("IMG_0042_small.jpg"), 0.75f);

        // Unrelated pictures with the same aspect ratio and different sizes: must not match.
        SampleImages.writeJpeg(SampleImages.scene(5, 800, 600), root.resolve("unrelated-a.jpg"), 0.9f);
        SampleImages.writeJpeg(SampleImages.scene(6, 400, 300), root.resolve("unrelated-b.jpg"), 0.9f);

        // Same base name but different pictures: needs review, never removed.
        SampleImages.writeJpeg(SampleImages.scene(7, 1000, 750), root.resolve("city.jpg"), 0.9f);
        SampleImages.writeJpeg(SampleImages.scene(8, 250, 188), root.resolve("city.md.jpg"), 0.8f);

        // The marked file is the larger one: keep it, remove the plain one, and say so.
        BufferedImage river = SampleImages.scene(10, 1000, 750);
        SampleImages.writeJpeg(river, root.resolve("river.md.jpg"), 0.9f);
        SampleImages.writeJpeg(SampleImages.resize(river, 200, 150), root.resolve("river.jpg"), 0.8f);

        // Not images, or not readable.
        SampleImages.writeBytes(root.resolve("clip.mp4"), new byte[] {0, 0, 0, 24, 'f', 't', 'y', 'p'});
        SampleImages.writeBytes(root.resolve("notes.md"), "# notes".getBytes(StandardCharsets.UTF_8));
        SampleImages.writeBytes(root.resolve("broken.jpg"), "garbage".getBytes(StandardCharsets.UTF_8));

        // Subfolder: a name match, and a thumbnail of mountain.jpg that lives in a different folder.
        BufferedImage lake = SampleImages.scene(9, 1200, 900);
        SampleImages.writeJpeg(lake, root.resolve("sub/lake.jpg"), 0.9f);
        SampleImages.writeJpeg(SampleImages.resize(lake, 320, 240), root.resolve("sub/lake.md.jpg"), 0.8f);
        SampleImages.writeJpeg(SampleImages.resize(mountain, 400, 300), root.resolve("sub/mountain-copy.jpg"), 0.8f);

        // Files already moved by an earlier run are never scanned.
        SampleImages.writeJpeg(SampleImages.resize(sunset, 120, 90), root.resolve("_removed/sunset.sm.jpg"), 0.8f);
    }
}
