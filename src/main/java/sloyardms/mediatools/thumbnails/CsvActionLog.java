package sloyardms.mediatools.thumbnails;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import sloyardms.mediatools.image.ImageInfo;

/** CSV log of what happened to each file. Written as UTF-8 with a BOM so Excel shows accented names correctly. */
public final class CsvActionLog implements Closeable {

    static final List<String> HEADER = List.of("timestamp", "action", "removed_file", "removed_dimensions",
            "kept_file", "kept_dimensions", "method", "hash_distance", "destination", "detail");

    static final char BOM = (char) 0xFEFF;

    private final BufferedWriter writer;
    private final Clock clock;

    public CsvActionLog(Path file, Clock clock) throws IOException {
        this.clock = clock;
        this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        writer.write(BOM);
        writeRow(HEADER);
    }

    public void log(String action, ImageInfo removed, ImageInfo kept, String method, int distance,
                    Path destination, String detail) throws IOException {
        String timestamp = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        writeRow(List.of(timestamp, action, removed.path().toString(), removed.dimensions(),
                kept.path().toString(), kept.dimensions(), method, Integer.toString(distance),
                destination == null ? "" : destination.toString(), detail == null ? "" : detail));
    }

    private void writeRow(List<String> fields) throws IOException {
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                writer.write(',');
            }
            writer.write(escape(fields.get(i)));
        }
        writer.write("\r\n");
        writer.flush();
    }

    static String escape(String field) {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
