package sloyardms.mediatools.thumbnails;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import sloyardms.mediatools.image.ImageInfo;

class CsvActionLogTest {

    @TempDir
    Path dir;

    @Test
    void writesHeaderAndEscapedRows() throws IOException {
        Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:15:30Z"), ZoneOffset.UTC);
        Path file = dir.resolve("log.csv");
        ImageInfo removed = new ImageInfo(Path.of("a, b.jpg"), 200, 150, "jpeg");
        ImageInfo kept = new ImageInfo(Path.of("a.jpg"), 1000, 750, "jpeg");

        try (CsvActionLog log = new CsvActionLog(file, clock)) {
            log.log("MOVED", removed, kept, "NAME", 2, Path.of("_removed/x.jpg"), "");
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(CsvActionLog.BOM + String.join(",", CsvActionLog.HEADER), lines.get(0));
        assertEquals("2026-10-23T10:15:30Z,MOVED,\"a, b.jpg\",200x150,a.jpg,1000x750,NAME,2,"
                + Path.of("_removed/x.jpg") + ",", lines.get(1));
    }

    @Test
    void leavesPlainFieldsUnquoted() {
        assertEquals("plain", CsvActionLog.escape("plain"));
        assertEquals("\"say \"\"hi\"\"\"", CsvActionLog.escape("say \"hi\""));
        assertEquals("\"line\nbreak\"", CsvActionLog.escape("line\nbreak"));
    }
}
