package team.terrafirmagrag.splice.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonLangFormatTest {

    @Test
    void parsesFlatJson() throws Exception {
        String json = """
        {
          "item.test.sword": "Iron Sword",
          "block.test.stone": "Stone"
        }
        """;
        Map<String, String> parsed =
                JsonLangFormat.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Iron Sword", parsed.get("item.test.sword"));
        assertEquals("Stone", parsed.get("block.test.stone"));
    }

    @Test
    void skipsNestedObjectsWithCallback() throws Exception {
        String json = """
        {
          "flat.key": "value",
          "nested": { "inner": "x" }
        }
        """;
        List<String> skipped = new ArrayList<>();
        Map<String, String> parsed = JsonLangFormat.parse(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), (key, msg) -> skipped.add(key));

        assertEquals("value", parsed.get("flat.key"));
        assertFalse(parsed.containsKey("nested"));
        assertEquals(List.of("nested"), skipped);
    }

    @Test
    void parsesTrailingCommasAndComments() throws Exception {
        String json = """
        {
          // comment
          "item.test.sword": "Iron Sword",
        }
        """;
        Map<String, String> parsed =
                JsonLangFormat.parse(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Iron Sword", parsed.get("item.test.sword"));
    }

    @Test
    void roundTripWrite() throws Exception {
        Map<String, String> original = Map.of("b.key", "B", "a.key", "A");
        byte[] bytes = JsonLangFormat.write(original);
        Map<String, String> parsed = JsonLangFormat.parse(new ByteArrayInputStream(bytes));
        assertEquals("A", parsed.get("a.key"));
        assertEquals("B", parsed.get("b.key"));
    }
}
