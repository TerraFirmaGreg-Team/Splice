package team.terrafirmagrag.splice.format;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.Logger;

@UtilityClass
public class JsonLangFormat {

  private final ObjectMapper MAPPER = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);

  public Map<String, String> parse(InputStream in) throws IOException {
    return parse(in, null);
  }

  public Map<String, String> parse(InputStream in, BiConsumer<String, String> onNestedSkip)
      throws IOException {
    JsonNode root = MAPPER.readTree(in);
    if (root == null || !root.isObject()) {
      return Map.of();
    }
    return parseObject((ObjectNode) root, onNestedSkip);
  }

  public Map<String, String> parse(Reader reader) throws IOException {
    return parse(reader, null);
  }

  public Map<String, String> parse(Reader reader, BiConsumer<String, String> onNestedSkip)
      throws IOException {
    JsonNode root = MAPPER.readTree(reader);
    if (root == null || !root.isObject()) {
      return Map.of();
    }
    return parseObject((ObjectNode) root, onNestedSkip);
  }

  public Map<String, String> parseFile(Path file) throws IOException {
    if (!Files.isRegularFile(file)) {
      return Map.of();
    }
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      return parse(reader);
    }
  }

  public byte[] write(Map<String, String> entries) {
    Map<String, String> sorted = new LinkedHashMap<>();
    entries.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(e -> sorted.put(e.getKey(), e.getValue()));
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(sorted);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public Map<String, String> parseObject(ObjectNode obj, BiConsumer<String, String> onNestedSkip) {
    Map<String, String> out = new LinkedHashMap<>();
    var fields = obj.fields();
    while (fields.hasNext()) {
      var entry = fields.next();
      JsonNode value = entry.getValue();
      if (value.isNull()) {
        continue;
      }
      if (value.isValueNode()) {
        out.put(entry.getKey(), value.asText());
      } else if (onNestedSkip != null) {
        onNestedSkip.accept(entry.getKey(), "nested JSON object is not valid in lang files");
      }
    }
    return out;
  }

  public BiConsumer<String, String> nestedWarningLogger(Logger logger) {
    return (key, message) -> logger.warn("Skipping lang key \"{}\": {}", key, message);
  }
}
