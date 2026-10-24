package sloyardms.mediatools.thumbnails;

import sloyardms.mediatools.image.ImageInfo;

/** A name match that is not safe to remove automatically. Neither file is touched. */
public record ReviewPair(ImageInfo larger, ImageInfo smaller, int distance, String reason) {
}
