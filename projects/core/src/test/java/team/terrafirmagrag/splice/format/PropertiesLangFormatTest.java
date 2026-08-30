package team.terrafirmagrag.splice.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertiesLangFormatTest {

    @Test
    void parsesCommentsAndBlankLines() throws Exception {
        String lang = """
        # comment line

        item.test.sword=Iron Sword
        block.test.stone=Stone
        """;
        Map<String, String> parsed =
                PropertiesLangFormat.parse(new ByteArrayInputStream(lang.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Iron Sword", parsed.get("item.test.sword"));
        assertEquals("Stone", parsed.get("block.test.stone"));
        assertEquals(2, parsed.size());
    }

    @Test
    void parsesEscapes() throws Exception {
        String lang = "key.with\\=equals=Line\\nTwo\\nLines\nplain=back\\\\slash";
        Map<String, String> parsed =
                PropertiesLangFormat.parse(new ByteArrayInputStream(lang.getBytes(StandardCharsets.UTF_8)));
        assertEquals("Line\nTwo\nLines", parsed.get("key.with=equals"));
        assertEquals("back\\slash", parsed.get("plain"));
    }

    @Test
    void roundTripWrite() {
        Map<String, String> original = Map.of("b.key", "B", "a.key", "A value");
        byte[] bytes = PropertiesLangFormat.write(original);
        String text = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(text.contains("a.key=A value"));
        assertTrue(text.contains("b.key=B"));
    }
}
