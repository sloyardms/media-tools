package sloyardms.mediatools.thumbnails;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Moves files into a separate folder, keeping their path relative to the scanned root. An existing file is never
 * overwritten; a numeric suffix is added instead ("photo-1.jpg").
 */
public final class FolderRemover implements Remover {

    private final Path root;
    private final Path removedDir;

    public FolderRemover(Path root, Path removedDir) {
        this.root = root.toAbsolutePath().normalize();
        this.removedDir = removedDir.toAbsolutePath().normalize();
    }

    @Override
    public String action() {
        return "MOVED";
    }

    @Override
    public String description() {
        return "to " + removedDir;
    }

    @Override
    public Path remove(Path file) throws IOException {
        Path source = file.toAbsolutePath().normalize();
        Path relative = source.startsWith(root) ? root.relativize(source) : source.getFileName();
        Path target = freeName(removedDir.resolve(relative));
        Files.createDirectories(target.getParent());
        return Files.move(source, target);
    }

    private static Path freeName(Path target) {
        if (!Files.exists(target)) {
            return target;
        }
        String name = target.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String extension = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; ; i++) {
            Path candidate = target.resolveSibling(stem + "-" + i + extension);
            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }
}
