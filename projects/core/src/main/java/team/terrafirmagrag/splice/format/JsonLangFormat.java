package team.terrafirmagrag.splice.format;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.experimental.UtilityClass;
import team.terrafirmagrag.splice.SpliceLog;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@UtilityClass
public class JsonLangFormat {

    private final JsonMapper MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .build();

    public Map<String, String> parse(InputStream in) {
        return parse(in, null);
    }

    public Map<String, String> parse(InputStream in, BiConsumer<String, String> onNestedSkip) {
        return fromTree(MAPPER.readTree(in), onNestedSkip);
    }

    private Map<String, String> fromTree(JsonNode root, BiConsumer<String, String> onNestedSkip) {
        if (root == null || !root.isObject()) {
            return Map.of();
        }
        return parseObject((ObjectNode) root, onNestedSkip);
    }

    public byte[] write(Map<String, String> entries) {
        Map<String, String> sorted = new LinkedHashMap<>();
        entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(sorted);
    }

    private Map<String, String> parseObject(ObjectNode obj, BiConsumer<String, String> onNestedSkip) {
        Map<String, String> out = new LinkedHashMap<>();
        for (var entry : obj.properties()) {
            JsonNode value = entry.getValue();
            if (value.isNull()) {
                continue;
            }
            if (value.isValueNode()) {
                out.put(entry.getKey(), value.asString());
            } else if (onNestedSkip != null) {
                onNestedSkip.accept(entry.getKey(), "nested JSON object is not valid in lang files");
            }
        }
        return out;
    }

    public BiConsumer<String, String> nestedWarningLogger() {
        return (key, message) -> SpliceLog.log.warn("Skipping lang key \"{}\": {}", key, message);
    }
}
