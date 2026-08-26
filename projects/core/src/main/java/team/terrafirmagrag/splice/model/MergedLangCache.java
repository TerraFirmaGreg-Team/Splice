package team.terrafirmagrag.splice.model;

import java.util.Map;

public final class MergedLangCache {

  private volatile Map<LocaleKey, MergedLangTable> tables = Map.of();

  public Map<LocaleKey, MergedLangTable> snapshot() {
    return tables;
  }

  public MergedLangTable get(LocaleKey key) {
    return tables.get(key);
  }

  public void replace(Map<LocaleKey, MergedLangTable> next) {
    tables = Map.copyOf(next);
  }
}
