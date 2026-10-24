package sloyardms.mediatools.thumbnails;

import sloyardms.mediatools.image.ImageInfo;

/**
 * A smaller image to remove and the larger image it duplicates.
 *
 * @param note  optional remark shown in the table, or an empty string
 */
public record DuplicatePair(ImageInfo kept, ImageInfo removed, MatchMethod method, int distance, String note) {

    public enum MatchMethod { NAME, HASH }
}
