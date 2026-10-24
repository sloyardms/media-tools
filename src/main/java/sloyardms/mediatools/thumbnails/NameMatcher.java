package sloyardms.mediatools.thumbnails;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import sloyardms.mediatools.image.DHash;
import sloyardms.mediatools.image.ImageFiles;

/**
 * Matches files by name: "photo.md.jpg" or "photo.md" is a thumbnail of "photo.&lt;any extension&gt;" in the same
 * folder. Every name match is also checked with dHash; if the pictures differ the pair goes to review instead.
 */
public final class NameMatcher {

    /** The base name of a file and whether it carries a thumbnail marker. Base names are lower-case. */
    public record NameKey(Path folder, String baseName, boolean marked) {
    }

    public record Result(List<DuplicatePair> duplicates, List<ReviewPair> needsReview) {
    }

    private final Set<String> markers;
    private final int maxDistance;

    public NameMatcher(Set<String> markers, int maxDistance) {
        this.markers = markers;
        this.maxDistance = maxDistance;
    }

    public NameKey parse(Path file) {
        String name = file.getFileName().toString();
        String stem = ImageFiles.hasImageExtension(file)
                ? name.substring(0, name.length() - ImageFiles.extension(file).length() - 1)
                : name;
        Path folder = file.toAbsolutePath().getParent();

        int dot = stem.lastIndexOf('.');
        if (dot > 0 && markers.contains(stem.substring(dot + 1).toLowerCase(Locale.ROOT))) {
            return new NameKey(folder, stem.substring(0, dot).toLowerCase(Locale.ROOT), true);
        }
        return new NameKey(folder, stem.toLowerCase(Locale.ROOT), false);
    }

    public Result match(List<HashedImage> images) {
        Map<NameKey, List<HashedImage>> groups = new LinkedHashMap<>();
        Map<Path, Boolean> marked = new HashMap<>();
        for (HashedImage image : images) {
            NameKey key = parse(image.path());
            marked.put(image.path(), key.marked());
            groups.computeIfAbsent(new NameKey(key.folder(), key.baseName(), false), k -> new ArrayList<>()).add(image);
        }

        List<DuplicatePair> duplicates = new ArrayList<>();
        List<ReviewPair> review = new ArrayList<>();
        for (List<HashedImage> group : groups.values()) {
            boolean hasMarked = group.stream().anyMatch(i -> marked.get(i.path()));
            boolean hasPlain = group.stream().anyMatch(i -> !marked.get(i.path()));
            if (hasMarked && hasPlain) {
                matchGroup(group, marked, duplicates, review);
            }
        }
        return new Result(duplicates, review);
    }

    /**
     * Walks the group from largest to smallest. Each image is compared with the larger images it name-matches
     * (at least one of the two has a marker) that are being kept, and pairs with the closest one.
     */
    private void matchGroup(List<HashedImage> group, Map<Path, Boolean> marked,
                            List<DuplicatePair> duplicates, List<ReviewPair> review) {
        List<HashedImage> sorted = new ArrayList<>(group);
        sorted.sort(Comparator.comparingLong(HashedImage::pixels).reversed()
                .thenComparing(i -> marked.get(i.path()))
                .thenComparing(HashedImage::path));

        Set<Path> removed = new HashSet<>();
        for (int i = 1; i < sorted.size(); i++) {
            HashedImage current = sorted.get(i);
            boolean currentMarked = marked.get(current.path());

            HashedImage closest = null;
            int closestDistance = Integer.MAX_VALUE;
            HashedImage sameSize = null;
            for (int j = 0; j < i; j++) {
                HashedImage other = sorted.get(j);
                if (removed.contains(other.path()) || !(currentMarked || marked.get(other.path()))) {
                    continue;
                }
                if (other.pixels() == current.pixels()) {
                    sameSize = other;
                    continue;
                }
                int distance = DHash.distance(other.hash(), current.hash());
                if (distance < closestDistance) {
                    closest = other;
                    closestDistance = distance;
                }
            }

            if (closest != null && closestDistance <= maxDistance) {
                String note = marked.get(closest.path()) && !currentMarked ? "kept file has the thumbnail name" : "";
                duplicates.add(new DuplicatePair(closest.info(), current.info(), DuplicatePair.MatchMethod.NAME,
                        closestDistance, note));
                removed.add(current.path());
            } else if (closest != null) {
                review.add(new ReviewPair(closest.info(), current.info(), closestDistance,
                        "images differ (distance " + closestDistance + " > " + maxDistance + ")"));
            } else if (sameSize != null) {
                review.add(new ReviewPair(sameSize.info(), current.info(),
                        DHash.distance(sameSize.hash(), current.hash()), "same dimensions"));
            }
        }
    }
}
