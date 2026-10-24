package sloyardms.mediatools.thumbnails;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import sloyardms.mediatools.menu.ConsoleIO;
import sloyardms.mediatools.menu.MenuAction;

/**
 * Finds images that are smaller versions of another image in the same folder and, after confirmation, moves the
 * smaller ones to the Recycle Bin (or a {@value ThumbnailOptions#REMOVED_DIR_NAME} folder).
 */
public final class RemoveThumbnailDuplicatesAction implements MenuAction {

    private static final DateTimeFormatter LOG_NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Function<Path, Remover> removerFactory;
    private final Clock clock;

    public RemoveThumbnailDuplicatesAction() {
        this(Remover::defaultFor, Clock.systemDefaultZone());
    }

    public RemoveThumbnailDuplicatesAction(Function<Path, Remover> removerFactory, Clock clock) {
        this.removerFactory = removerFactory;
        this.clock = clock;
    }

    @Override
    public String label() {
        return "Remove thumbnail duplicates";
    }

    @Override
    public void run(ConsoleIO io) throws IOException {
        Path root = io.askExistingDirectory("Folder to scan: ");
        boolean recursive = io.askYesNo("Include subfolders?", true);
        int nameMaxDistance = io.askInt("Max dHash distance to confirm a name match (0-64)",
                ThumbnailOptions.DEFAULT_NAME_MATCH_MAX_DISTANCE, 0, 64);
        int hashMaxDistance = io.askInt("Max dHash distance for a hash-only match (0-64)",
                ThumbnailOptions.DEFAULT_HASH_MATCH_MAX_DISTANCE, 0, 64);
        double tolerancePercent = io.askDouble("Aspect ratio tolerance for hash matches, in %",
                ThumbnailOptions.DEFAULT_ASPECT_TOLERANCE * 100, 0, 50);
        ThumbnailOptions options = new ThumbnailOptions(root, recursive, nameMaxDistance, hashMaxDistance,
                tolerancePercent / 100, ThumbnailOptions.DEFAULT_MARKERS);

        DuplicateFinder.Result result = new DuplicateFinder(options).find(new ConsoleProgress(io));
        io.printf("Scanned %d image(s).%n", result.imagesScanned());
        PairTable.print(io, options, result);

        if (result.duplicates().isEmpty()) {
            io.println();
            io.println("Nothing to remove.");
            return;
        }

        Remover remover = removerFactory.apply(root);
        io.println();
        io.println("Dry run: nothing has been changed yet.");
        if (!io.askYesNo("Move " + result.duplicates().size() + " file(s) " + remover.description() + "?", false)) {
            io.println("Cancelled. No files were changed.");
            return;
        }

        Path logFile = root.resolve("thumbnail-cleanup-" + LocalDateTime.now(clock).format(LOG_NAME_TIME) + ".csv");
        int removed = 0;
        int failed = 0;
        try (CsvActionLog log = new CsvActionLog(logFile, clock)) {
            for (DuplicatePair pair : result.duplicates()) {
                try {
                    Path destination = remover.remove(pair.removed().path());
                    log.log(remover.action(), pair.removed(), pair.kept(), pair.method().name(), pair.distance(),
                            destination, pair.note());
                    removed++;
                } catch (IOException | RuntimeException e) {
                    log.log("FAILED", pair.removed(), pair.kept(), pair.method().name(), pair.distance(),
                            null, e.getMessage());
                    io.println("Failed: " + pair.removed().path() + " - " + e.getMessage());
                    failed++;
                }
            }
            for (ReviewPair pair : result.needsReview()) {
                log.log("NEEDS_REVIEW", pair.smaller(), pair.larger(), "NAME", pair.distance(), null, pair.reason());
            }
        }

        io.printf("Removed %d file(s), %d failed. Log: %s%n", removed, failed, logFile);
    }

    /** Prints a single updating progress line while images are hashed. */
    private static final class ConsoleProgress implements DuplicateFinder.Progress {

        private final ConsoleIO io;
        private int total;
        private int lastPercent = -1;

        ConsoleProgress(ConsoleIO io) {
            this.io = io;
        }

        @Override
        public void started(int total) {
            this.total = total;
            io.printf("Reading %d candidate file(s)...%n", total);
        }

        @Override
        public synchronized void processed(int done) {
            int percent = total == 0 ? 100 : done * 100 / total;
            if (percent > lastPercent) {
                lastPercent = percent;
                io.out().print("\r  " + done + "/" + total + " (" + percent + "%)");
                if (done == total) {
                    io.println();
                }
                io.out().flush();
            }
        }
    }
}
