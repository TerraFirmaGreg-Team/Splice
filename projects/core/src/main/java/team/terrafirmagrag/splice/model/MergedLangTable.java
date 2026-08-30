package team.terrafirmagrag.splice.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record MergedLangTable(Map<String, String> entries) {

    public MergedLangTable(Map<String, String> entries) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
