package sloyardms.mediatools.thumbnails;

import java.nio.file.Path;

import sloyardms.mediatools.image.ImageInfo;

/** A scanned image with its dHash. */
public record HashedImage(ImageInfo info, long hash) {

    public Path path() {
        return info.path();
    }

    public long pixels() {
        return info.pixels();
    }
}
