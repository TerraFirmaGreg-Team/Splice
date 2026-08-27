package team.terrafirmagrag.splice.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PropertiesLangFormat {

  public Map<String, String> parse(InputStream in) throws IOException {
    Map<String, String> out = new LinkedHashMap<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      parseLines(reader, out);
    }
    return out;
  }

  public Map<String, String> parseFile(Path file) throws IOException {
    if (!Files.isRegularFile(file)) {
      return Map.of();
    }
    try (InputStream in = Files.newInputStream(file)) {
      return parse(in);
    }
  }

  public byte[] write(Map<String, String> entries) {
    StringWriter writer = new StringWriter();
    entries.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            e -> {
              writer.write(escapeKey(e.getKey()));
              writer.write('=');
              writer.write(escapeValue(e.getValue()));
              writer.write('\n');
            });
    return writer.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void parseLines(BufferedReader reader, Map<String, String> out) throws IOException {
    String line;
    while ((line = reader.readLine()) != null) {
      line = line.stripLeading();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      int eq = indexOfUnescapedEquals(line);
      if (eq < 0) {
        continue;
      }
      String key = unescape(line.substring(0, eq).stripTrailing());
      String value = unescape(line.substring(eq + 1).stripLeading());
      if (!key.isEmpty()) {
        out.put(key, value);
      }
    }
  }

  private int indexOfUnescapedEquals(String line) {
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '\\') {
        i++;
        continue;
      }
      if (c == '=') {
        return i;
      }
    }
    return -1;
  }

  private String escapeKey(String key) {
    return key.replace("\\", "\\\\").replace("=", "\\=");
  }

  private String escapeValue(String value) {
    return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
  }

  private String unescape(String text) {
    StringBuilder sb = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '\\' && i + 1 < text.length()) {
        char next = text.charAt(++i);
        switch (next) {
          case 'n' -> sb.append('\n');
          case 'r' -> sb.append('\r');
          case '\\' -> sb.append('\\');
          case '=' -> sb.append('=');
          default -> sb.append(next);
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
