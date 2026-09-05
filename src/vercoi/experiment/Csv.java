package vercoi.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class Csv {
    private Csv() {}
    static void write(Path p, List<String[]> rows) throws IOException {
        Files.createDirectories(p.getParent());
        try (var w = Files.newBufferedWriter(p)) {
            for (String[] r : rows) {
                for (int i = 0; i < r.length; i++) {
                    if (i > 0) w.write(',');
                    String s = r[i] == null ? "" : r[i];
                    if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                        w.write('"');
                        w.write(s.replace("\"", "\"\""));
                        w.write('"');
                    } else w.write(s);
                }
                w.newLine();
            }
        }
    }
}
