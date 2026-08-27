package team.terrafirmagrag.splice.merge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LangFragmentMerger {

  public Map<String, String> merge(
      MergePolicy policy, Optional<Map<String, String>> flat, List<Map<String, String>> fragments) {
    Map<String, String> merged = new LinkedHashMap<>();
    flat.ifPresent(layer -> mergeInto(policy, merged, layer));
    for (Map<String, String> fragment : fragments) {
      mergeInto(policy, merged, fragment);
    }
    return merged;
  }

  public void mergeInto(MergePolicy policy, Map<String, String> into, Map<String, String> layer) {
    if (layer.isEmpty()) {
      return;
    }
    for (Map.Entry<String, String> entry : layer.entrySet()) {
      String key = entry.getKey();
      if (key == null || policy.shouldSkipKey(key)) {
        continue;
      }
      String value = entry.getValue();
      if (value == null) {
        continue;
      }
      String previous = into.put(key, value);
      if (previous != null && !previous.equals(value)) {
        policy.onDuplicateOverride().accept(key, previous + " → " + value);
      }
    }
  }
}
