package sloyardms.mediatools.image;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** File-name helpers and folder scanning. */
public final class ImageFiles {

    /** Extensions treated as images. Anything else (videos, sidecar files, ...) is skipped. */
    public static final Set<String> IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "tif", "tiff");

    private ImageFiles() {
    }

    /** Lower-case extension without the dot, or an empty string if there is none. */
    public static String extension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 && dot < name.length() - 1 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    public static boolean hasImageExtension(Path file) {
        return IMAGE_EXTENSIONS.contains(extension(file));
    }

    /**
     * Lists regular files under {@code root}, sorted by path.
     *
     * @param recursive    whether to descend into subfolders
     * @param skippedDirs  folder names that are never entered (compared case-insensitively)
     */
    public static List<Path> listFiles(Path root, boolean recursive, Set<String> skippedDirs) throws IOException {
        Set<String> skipped = lowerCase(skippedDirs);
        List<Path> files = new ArrayList<>();
        int depth = recursive ? Integer.MAX_VALUE : 1;

        Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), depth, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(root) && skipped.contains(dir.getFileName().toString().toLowerCase(Locale.ROOT))) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) {
                // Unreadable entries (permissions, broken links) are simply left out.
                return FileVisitResult.CONTINUE;
            }
        });

        files.sort(null);
        return files;
    }

    private static Set<String> lowerCase(Set<String> names) {
        Set<String> result = new HashSet<>();
        for (String name : names) {
            result.add(name.toLowerCase(Locale.ROOT));
        }
        return result;
    }
}
