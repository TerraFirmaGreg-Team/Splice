package team.terrafirmagrag.splice.model;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import team.terrafirmagrag.splice.util.LangPaths;

public final class MergedLangCache {

    private volatile Map<LocaleKey, MergedLangTable> tables = Map.of();

    public Map<LocaleKey, MergedLangTable> snapshot() {
        return tables;
    }

    public MergedLangTable tableFor(String namespace, String path, String extension) {
        String locale = LangPaths.flatLocale(path, extension);
        return locale == null ? null : tables.get(new LocaleKey(namespace, locale));
    }

    public Set<String> namespaces() {
        return tables.keySet().stream().map(LocaleKey::namespace).collect(Collectors.toUnmodifiableSet());
    }

    public void replace(Map<LocaleKey, MergedLangTable> next) {
        tables = Map.copyOf(next);
    }
}
