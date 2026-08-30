package team.terrafirmagrag.splice.merge;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.experimental.UtilityClass;
import team.terrafirmagrag.splice.format.JsonLangFormat;
import team.terrafirmagrag.splice.format.PropertiesLangFormat;
import team.terrafirmagrag.splice.model.Extra;
import team.terrafirmagrag.splice.model.LangSource;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergePolicy;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.util.LangPaths;

@UtilityClass
public class LangMergePipeline {

    public Map<LocaleKey, MergedLangTable> merge(
            Iterable<Path> packRoots, Extra extra, String extension, MergePolicy policy) {
        String suffix = extension.startsWith(".") ? extension : "." + extension;
        String ext = suffix.substring(1).toLowerCase(Locale.ROOT);
        LangIndex index = LangIndex.of(packRoots, extra, suffix);

        Map<LocaleKey, MergedLangTable> out = new HashMap<>();
        for (String namespace : index.namespaces()) {
            Set<String> assetPaths = index.paths(namespace);
            for (String locale : LangPaths.fragmentLocalesFrom(assetPaths)) {
                List<String> fragmentPaths =
                        LangPaths.filterResourcePaths(assetPaths, LangPaths.fragmentFolderPrefix(locale), suffix);
                if (fragmentPaths.isEmpty()) {
                    continue;
                }
                List<Map<String, String>> fragments = new ArrayList<>();
                for (String path : fragmentPaths) {
                    Map<String, String> layer = overlay(namespace, path, extra, index, ext, policy);
                    if (!layer.isEmpty()) {
                        fragments.add(layer);
                    }
                }
                if (fragments.isEmpty()) {
                    continue;
                }
                Map<String, String> flat =
                        overlay(namespace, LangPaths.flatPath(locale, ext), extra, index, ext, policy);
                Map<String, String> merged = LangFragmentMerger.merge(policy, Optional.of(flat), fragments);
                if (!merged.isEmpty()) {
                    out.put(new LocaleKey(namespace, locale), new MergedLangTable(merged));
                }
            }
        }
        return out;
    }

    private Map<String, String> overlay(
            String namespace, String path, Extra extra, LangIndex index, String extension, MergePolicy policy) {
        Map<String, String> layer = extra.get(namespace, path);
        if (!layer.isEmpty()) {
            return layer;
        }
        LangSource source = index.source(namespace, path);
        return source == null ? Map.of() : read(source, extension, policy);
    }

    private Map<String, String> read(LangSource source, String extension, MergePolicy policy) {
        try (InputStream in = open(source)) {
            Map<String, String> parsed =
                    "json".equals(extension) ? JsonLangFormat.parse(in) : PropertiesLangFormat.parse(in);
            Map<String, String> filtered = new LinkedHashMap<>();
            LangFragmentMerger.mergeInto(policy, filtered, parsed);
            return filtered;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private InputStream open(LangSource source) throws IOException {
        if (source.file() != null) {
            return Files.newInputStream(source.file());
        }
        return new ByteArrayInputStream(LangPaths.readJarEntry(source.zip().toFile(), source.zipEntry()));
    }
}
