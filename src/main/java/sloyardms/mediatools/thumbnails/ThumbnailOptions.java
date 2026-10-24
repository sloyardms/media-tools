package sloyardms.mediatools.thumbnails;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Settings for one thumbnail-duplicate scan.
 *
 * @param hashThreshold     maximum dHash hamming distance (0-64) for two images to count as the same picture
 * @param aspectTolerance   maximum relative aspect-ratio difference for hash-only matches, e.g. 0.02 for 2%
 * @param thumbnailMarkers  name segments marking a smaller version, e.g. "md" in "photo.md.jpg"
 */
public record ThumbnailOptions(Path root, boolean recursive, int hashThreshold, double aspectTolerance,
                               Set<String> thumbnailMarkers) {

    public static final int DEFAULT_HASH_THRESHOLD = 6;
    public static final double DEFAULT_ASPECT_TOLERANCE = 0.02;
    public static final Set<String> DEFAULT_MARKERS = Set.of("md", "sm", "thumb");

    /** Folder used when the Recycle Bin is unavailable; it is never scanned. */
    public static final String REMOVED_DIR_NAME = "_removed";

    public ThumbnailOptions {
        thumbnailMarkers = thumbnailMarkers.stream()
                .map(m -> m.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static ThumbnailOptions defaults(Path root, boolean recursive) {
        return new ThumbnailOptions(root, recursive, DEFAULT_HASH_THRESHOLD, DEFAULT_ASPECT_TOLERANCE, DEFAULT_MARKERS);
    }
}
