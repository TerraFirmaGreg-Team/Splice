package team.terrafirmagrag.splice.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class MergedLangTable {

  private final Map<String, String> entries;

  public MergedLangTable(Map<String, String> entries) {
    this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
  }

  public boolean isEmpty() {
    return entries.isEmpty();
  }
}
