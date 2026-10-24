package sloyardms.mediatools.thumbnails;

import java.io.IOException;
import java.nio.file.Path;

/** Takes a file out of the gallery without deleting it permanently. */
public interface Remover {

    /** Action name written to the CSV log, e.g. TRASHED. */
    String action();

    /** Human-readable destination for the confirmation prompt, e.g. "to the Recycle Bin". */
    String description();

    /**
     * Removes the file.
     *
     * @return where the file went, or {@code null} if the location is not known (Recycle Bin)
     */
    Path remove(Path file) throws IOException;

    /** Recycle Bin when the desktop supports it, otherwise a {@value ThumbnailOptions#REMOVED_DIR_NAME} folder. */
    static Remover defaultFor(Path root) {
        return TrashRemover.isSupported()
                ? new TrashRemover()
                : new FolderRemover(root, root.resolve(ThumbnailOptions.REMOVED_DIR_NAME));
    }
}
