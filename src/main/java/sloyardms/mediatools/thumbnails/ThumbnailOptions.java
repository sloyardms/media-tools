package sloyardms.mediatools.thumbnails;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Settings for one thumbnail-duplicate scan. Distances are dHash hamming distances (0-64 differing bits).
 *
 * @param nameMatchMaxDistance  maximum distance for a name match to be removed; above it the pair needs review
 * @param hashMatchMaxDistance  maximum distance for two images without the naming pattern to match
 * @param aspectTolerance       maximum relative aspect-ratio difference for hash-only matches, e.g. 0.02 for 2%
 * @param thumbnailMarkers      name segments marking a smaller version, e.g. "md" in "photo.md.jpg"
 */
public record ThumbnailOptions(Path root, boolean recursive, int nameMatchMaxDistance, int hashMatchMaxDistance,
                               double aspectTolerance, Set<String> thumbnailMarkers) {

    public static final int DEFAULT_NAME_MATCH_MAX_DISTANCE = 10;
    public static final int DEFAULT_HASH_MATCH_MAX_DISTANCE = 6;
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
        return new ThumbnailOptions(root, recursive, DEFAULT_NAME_MATCH_MAX_DISTANCE, DEFAULT_HASH_MATCH_MAX_DISTANCE,
                DEFAULT_ASPECT_TOLERANCE, DEFAULT_MARKERS);
    }
}
