package sloyardms.mediatools.thumbnails;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;

/** Moves files to the operating system's trash (the Recycle Bin on Windows). */
public final class TrashRemover implements Remover {

    public static boolean isSupported() {
        try {
            return !GraphicsEnvironment.isHeadless()
                    && Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public String action() {
        return "TRASHED";
    }

    @Override
    public String description() {
        return "to the Recycle Bin";
    }

    @Override
    public Path remove(Path file) throws IOException {
        if (!Desktop.getDesktop().moveToTrash(file.toFile())) {
            throw new IOException("Could not move to the Recycle Bin: " + file);
        }
        return null;
    }
}
