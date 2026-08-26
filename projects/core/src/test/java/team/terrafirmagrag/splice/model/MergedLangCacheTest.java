package team.terrafirmagrag.splice.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MergedLangCacheTest {

  @Test
  void replaceAndSnapshot() {
    MergedLangCache cache = new MergedLangCache();
    LocaleKey key = new LocaleKey("mymod", "en_us");
    MergedLangTable table = new MergedLangTable(Map.of("a", "1"));

    cache.replace(Map.of(key, table));

    assertEquals(table, cache.get(key));
    assertEquals(1, cache.snapshot().size());
  }

  @Test
  void replaceEmptyClearsEntries() {
    MergedLangCache cache = new MergedLangCache();
    LocaleKey key = new LocaleKey("mymod", "en_us");
    cache.replace(Map.of(key, new MergedLangTable(Map.of())));
    cache.replace(Map.of());
    assertNull(cache.get(key));
    assertEquals(0, cache.snapshot().size());
  }
}
