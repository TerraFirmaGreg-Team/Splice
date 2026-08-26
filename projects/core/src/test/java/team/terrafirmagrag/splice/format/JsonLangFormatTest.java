package team.terrafirmagrag.splice.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonLangFormatTest {

  @Test
  void parsesFlatJson() throws Exception {
    String json =
        """
        {
          "item.test.sword": "Iron Sword",
          "block.test.stone": "Stone"
        }
        """;
    Map<String, String> parsed = JsonLangFormat.parse(new StringReader(json));
    assertEquals("Iron Sword", parsed.get("item.test.sword"));
    assertEquals("Stone", parsed.get("block.test.stone"));
  }

  @Test
  void skipsNestedObjectsWithCallback() {
    ObjectNode root = JsonNodeFactory.instance.objectNode();
    root.put("flat.key", "value");
    root.set("nested", JsonNodeFactory.instance.objectNode().put("inner", "x"));

    List<String> skipped = new ArrayList<>();
    Map<String, String> parsed = JsonLangFormat.parseObject(root, (key, msg) -> skipped.add(key));

    assertEquals("value", parsed.get("flat.key"));
    assertFalse(parsed.containsKey("nested"));
    assertEquals(List.of("nested"), skipped);
  }

  @Test
  void roundTripWrite() throws Exception {
    Map<String, String> original = Map.of("b.key", "B", "a.key", "A");
    byte[] bytes = JsonLangFormat.write(original);
    Map<String, String> parsed = JsonLangFormat.parse(new ByteArrayInputStream(bytes));
    assertEquals("A", parsed.get("a.key"));
    assertEquals("B", parsed.get("b.key"));
  }

  @Test
  void parseFileFromDisk(@TempDir Path temp) throws Exception {
    Path file = temp.resolve("en_us.json");
    Files.writeString(file, "{\"item.foo\": \"Foo\"}", StandardCharsets.UTF_8);
    Map<String, String> parsed = JsonLangFormat.parseFile(file);
    assertEquals("Foo", parsed.get("item.foo"));
  }
}
