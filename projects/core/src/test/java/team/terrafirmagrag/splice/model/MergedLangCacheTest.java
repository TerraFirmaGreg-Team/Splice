package team.terrafirmagrag.splice.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MergedLangCacheTest {

    @Test
    void replaceAndSnapshot() {
        MergedLangCache cache = new MergedLangCache();
        LocaleKey key = new LocaleKey("mymod", "en_us");
        MergedLangTable table = new MergedLangTable(Map.of("a", "1"));

        cache.replace(Map.of(key, table));

        assertEquals(table, cache.snapshot().get(key));
        assertEquals(1, cache.snapshot().size());
        assertEquals(Set.of("mymod"), cache.namespaces());
    }

    @Test
    void replaceEmptyClearsEntries() {
        MergedLangCache cache = new MergedLangCache();
        LocaleKey key = new LocaleKey("mymod", "en_us");
        cache.replace(Map.of(key, new MergedLangTable(Map.of())));
        cache.replace(Map.of());
        assertNull(cache.snapshot().get(key));
        assertEquals(0, cache.snapshot().size());
    }

    @Test
    void tableForLooksUpFlatLangPath() {
        MergedLangCache cache = new MergedLangCache();
        LocaleKey key = new LocaleKey("mymod", "en_us");
        MergedLangTable table = new MergedLangTable(Map.of("a", "1"));
        cache.replace(Map.of(key, table));

        assertEquals(table, cache.tableFor("mymod", "lang/en_us.json", "json"));
        assertNull(cache.tableFor("mymod", "lang/en_us/items.json", "json"));
        assertNull(cache.tableFor("other", "lang/en_us.json", "json"));
        assertNull(cache.tableFor("mymod", "textures/block.png", "json"));
    }
}
