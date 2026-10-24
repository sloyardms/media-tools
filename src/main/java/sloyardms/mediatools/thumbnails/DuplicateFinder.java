package sloyardms.mediatools.thumbnails;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import sloyardms.mediatools.image.DHash;
import sloyardms.mediatools.image.ImageFiles;

/** Scans a folder and finds thumbnails that duplicate a larger image, first by name and then by dHash. */
public final class DuplicateFinder {

    public record Result(List<DuplicatePair> duplicates, List<ReviewPair> needsReview, List<Path> unreadable,
                         int imagesScanned) {
    }

    /** Receives the number of candidate files once, then the count of files processed so far. */
    public interface Progress {
        Progress NONE = new Progress() {
            @Override
            public void started(int total) {
            }

            @Override
            public void processed(int done) {
            }
        };

        void started(int total);

        void processed(int done);
    }

    private final ThumbnailOptions options;

    public DuplicateFinder(ThumbnailOptions options) {
        this.options = options;
    }

    public Result find(Progress progress) throws IOException {
        List<Path> candidates = new ArrayList<>();
        for (Path file : ImageFiles.listFiles(options.root(), options.recursive(),
                Set.of(ThumbnailOptions.REMOVED_DIR_NAME))) {
            // "photo.md" has no image extension; it is probed and kept only if it decodes as an image.
            if (ImageFiles.hasImageExtension(file) || options.thumbnailMarkers().contains(ImageFiles.extension(file))) {
                candidates.add(file);
            }
        }

        progress.started(candidates.size());
        ConcurrentLinkedQueue<HashedImage> hashed = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Path> unreadable = new ConcurrentLinkedQueue<>();
        AtomicInteger done = new AtomicInteger();
        candidates.parallelStream().forEach(file -> {
            try {
                DHash.Result result = DHash.compute(file);
                hashed.add(new HashedImage(result.info(), result.hash()));
            } catch (IOException | RuntimeException e) {
                if (ImageFiles.hasImageExtension(file)) {
                    unreadable.add(file);
                }
            }
            progress.processed(done.incrementAndGet());
        });

        List<HashedImage> images = new ArrayList<>(hashed);
        images.sort(Comparator.comparing(HashedImage::path));
        List<Path> unreadableSorted = new ArrayList<>(unreadable);
        unreadableSorted.sort(null);

        return match(images, unreadableSorted);
    }

    /** Matching step on already hashed images, exposed for tests. */
    public Result match(List<HashedImage> images, List<Path> unreadable) {
        NameMatcher.Result byName = new NameMatcher(options.thumbnailMarkers(), options.hashThreshold()).match(images);

        Set<Path> removedByName = new HashSet<>();
        Set<Path> protectedFiles = new HashSet<>();
        for (DuplicatePair pair : byName.duplicates()) {
            removedByName.add(pair.removed().path());
            protectedFiles.add(pair.kept().path());
        }
        for (ReviewPair pair : byName.needsReview()) {
            protectedFiles.add(pair.larger().path());
            protectedFiles.add(pair.smaller().path());
        }

        List<HashedImage> remaining = images.stream().filter(i -> !removedByName.contains(i.path())).toList();
        List<DuplicatePair> byHash = new HashMatcher(options.hashThreshold(), options.aspectTolerance())
                .match(remaining, protectedFiles);

        List<DuplicatePair> duplicates = new ArrayList<>(byName.duplicates());
        duplicates.addAll(byHash);
        duplicates.sort(Comparator.comparing((DuplicatePair p) -> p.kept().path()).thenComparing(p -> p.removed().path()));
        List<ReviewPair> review = new ArrayList<>(byName.needsReview());
        review.sort(Comparator.comparing((ReviewPair p) -> p.larger().path()).thenComparing(p -> p.smaller().path()));

        return new Result(duplicates, review, unreadable, images.size());
    }
}
