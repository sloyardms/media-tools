package sloyardms.mediatools.thumbnails;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sloyardms.mediatools.image.DHash;

/**
 * Fallback for files without the naming pattern: images in the same folder with a similar aspect ratio and a
 * dHash distance within the threshold. The larger one is kept.
 */
public final class HashMatcher {

    private final int hashThreshold;
    private final double aspectTolerance;

    public HashMatcher(int hashThreshold, double aspectTolerance) {
        this.hashThreshold = hashThreshold;
        this.aspectTolerance = aspectTolerance;
    }

    /**
     * @param images     candidates, typically everything not already removed by name
     * @param protectedFiles  files that may be kept as originals but are never removed (name-match keepers,
     *                   files under review)
     */
    public List<DuplicatePair> match(List<HashedImage> images, Set<Path> protectedFiles) {
        Map<Path, List<HashedImage>> byFolder = new LinkedHashMap<>();
        for (HashedImage image : images) {
            byFolder.computeIfAbsent(image.path().toAbsolutePath().getParent(), k -> new ArrayList<>()).add(image);
        }

        List<DuplicatePair> duplicates = new ArrayList<>();
        for (List<HashedImage> folder : byFolder.values()) {
            matchFolder(folder, protectedFiles, duplicates);
        }
        return duplicates;
    }

    /** Largest first: each image still unassigned keeps every smaller match that is not yet assigned. */
    private void matchFolder(List<HashedImage> folder, Set<Path> protectedFiles, List<DuplicatePair> out) {
        List<HashedImage> sorted = new ArrayList<>(folder);
        sorted.sort(Comparator.comparingLong(HashedImage::pixels).reversed().thenComparing(HashedImage::path));

        Set<Path> assigned = new HashSet<>();
        for (int i = 0; i < sorted.size(); i++) {
            HashedImage kept = sorted.get(i);
            if (assigned.contains(kept.path())) {
                continue;
            }
            for (int j = i + 1; j < sorted.size(); j++) {
                HashedImage candidate = sorted.get(j);
                if (candidate.pixels() >= kept.pixels()
                        || assigned.contains(candidate.path())
                        || protectedFiles.contains(candidate.path())
                        || !similarAspect(kept, candidate)) {
                    continue;
                }
                int distance = DHash.distance(kept.hash(), candidate.hash());
                if (distance <= hashThreshold) {
                    out.add(new DuplicatePair(kept.info(), candidate.info(), DuplicatePair.MatchMethod.HASH, distance, ""));
                    assigned.add(candidate.path());
                }
            }
        }
    }

    private boolean similarAspect(HashedImage a, HashedImage b) {
        double ra = a.info().aspectRatio();
        double rb = b.info().aspectRatio();
        return Math.abs(ra - rb) / Math.max(ra, rb) <= aspectTolerance;
    }
}
