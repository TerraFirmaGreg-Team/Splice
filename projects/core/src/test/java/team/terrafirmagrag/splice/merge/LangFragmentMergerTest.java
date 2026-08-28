package team.terrafirmagrag.splice.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import team.terrafirmagrag.splice.model.MergePolicy;

class LangFragmentMergerTest {

  @Test
  void flatThenFragmentsLastWins() {
    Map<String, String> flat = Map.of("a", "flat", "b", "flat-b", "c", "flat-c");
    Map<String, String> frag1 = Map.of("b", "frag1-b");
    Map<String, String> frag2 = Map.of("b", "frag2-b", "d", "frag2-d");

    Map<String, String> merged =
        LangFragmentMerger.merge(MergePolicy.defaults(), Optional.of(flat), List.of(frag1, frag2));

    assertEquals("flat", merged.get("a"));
    assertEquals("frag2-b", merged.get("b"));
    assertEquals("flat-c", merged.get("c"));
    assertEquals("frag2-d", merged.get("d"));
  }

  @Test
  void skipsDoubleUnderscoreMetadataKeys() {
    Map<String, String> flat = Map.of("__comment", "meta", "item.foo", "Foo", "__version", "1");
    Map<String, String> frag = Map.of("__author", "test", "item.bar", "Bar");

    Map<String, String> merged =
        LangFragmentMerger.merge(MergePolicy.defaults(), Optional.of(flat), List.of(frag));

    assertFalse(merged.containsKey("__comment"));
    assertFalse(merged.containsKey("__version"));
    assertFalse(merged.containsKey("__author"));
    assertEquals("Foo", merged.get("item.foo"));
    assertEquals("Bar", merged.get("item.bar"));
  }

  @Test
  void flatOnlyWhenNoFragments() {
    Map<String, String> flat = Map.of("key.one", "One");

    Map<String, String> merged =
        LangFragmentMerger.merge(MergePolicy.defaults(), Optional.of(flat), List.of());

    assertEquals(Map.of("key.one", "One"), merged);
  }

  @Test
  void fragmentsOnlyWhenNoFlat() {
    Map<String, String> frag = Map.of("key.one", "FromFragment");

    Map<String, String> merged =
        LangFragmentMerger.merge(MergePolicy.defaults(), Optional.empty(), List.of(frag));

    assertEquals("FromFragment", merged.get("key.one"));
  }

  @Test
  void duplicateOverrideInvokesCallback() {
    Map<String, String> flat = Map.of("shared", "first");
    Map<String, String> frag = Map.of("shared", "second");
    List<String> warnings = new ArrayList<>();

    MergePolicy policy = new MergePolicy(true, (key, message) -> warnings.add(key + ":" + message));

    LangFragmentMerger.merge(policy, Optional.of(flat), List.of(frag));

    assertEquals(1, warnings.size());
    assertTrue(warnings.get(0).startsWith("shared:"));
  }

  @Test
  void emptySourceReturnsEmptyMap() {
    Map<String, String> merged =
        LangFragmentMerger.merge(MergePolicy.defaults(), Optional.empty(), List.of());
    assertTrue(merged.isEmpty());
  }
}
