package team.terrafirmagrag.splice.merge;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import team.terrafirmagrag.splice.model.Extra;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergePolicy;
import team.terrafirmagrag.splice.model.MergedLangTable;

class LangMergePipelineTest {

    private static Map<LocaleKey, MergedLangTable> merge(List<Path> roots, Extra extra) {
        return LangMergePipeline.merge(roots, extra, "json", MergePolicy.defaults());
    }

    private static Extra extra(String namespace, String path, Map<String, String> layer) {
        return new Extra() {
            @Override
            public Map<String, String> get(String ns, String resourcePath) {
                return namespace.equals(ns) && path.equalsIgnoreCase(resourcePath) ? layer : Map.of();
            }

            @Override
            public void extraPaths(BiConsumer<String, String> sink) {
                sink.accept(namespace, path);
            }
        };
    }

    private static String value(Map<LocaleKey, MergedLangTable> tables, String namespace, String locale, String key) {
        return tables.get(new LocaleKey(namespace, locale)).entries().get(key);
    }

    private static Path packWithAssets(Path temp, String namespace) throws Exception {
        Path root = temp.resolve("pack");
        Files.createDirectories(root.resolve("assets").resolve(namespace).resolve("lang"));
        return root;
    }

    private static void write(Path file, String json) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, json, StandardCharsets.UTF_8);
    }

    private static void put(ZipOutputStream zip, String name, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Test
    void flatOnlyYieldsNoTable(@TempDir Path temp) throws Exception {
        Path root = packWithAssets(temp, "tfg");
        write(root.resolve("assets/tfg/lang/en_us.json"), "{\"item.tfg.x\":\"X\"}");

        Map<LocaleKey, MergedLangTable> tables = merge(List.of(root), Extra.NONE);

        assertFalse(tables.containsKey(new LocaleKey("tfg", "en_us")));
    }

    @Test
    void extraOnlyFragmentAppearsInTable(@TempDir Path temp) {
        Map<LocaleKey, MergedLangTable> tables =
                merge(List.of(temp), extra("tfg", "lang/en_us/a.json", Map.of("item.tfg.x", "X")));

        assertEquals("X", value(tables, "tfg", "en_us", "item.tfg.x"));
    }

    @Test
    void nonemptyExtraWinsOverDisk(@TempDir Path temp) throws Exception {
        Path root = packWithAssets(temp, "tfg");
        write(root.resolve("assets/tfg/lang/en_us/a.json"), "{\"item.tfg.x\":\"disk\"}");

        Map<LocaleKey, MergedLangTable> tables =
                merge(List.of(root), extra("tfg", "lang/en_us/a.json", Map.of("item.tfg.x", "extra")));

        assertEquals("extra", value(tables, "tfg", "en_us", "item.tfg.x"));
    }

    @Test
    void emptyExtraFallsThroughToMixedCaseDisk(@TempDir Path temp) throws Exception {
        Path root = packWithAssets(temp, "tfg");
        write(root.resolve("assets/tfg/lang/en_us/Quests/a.json"), "{\"item.tfg.quest\":\"Q\"}");

        Map<LocaleKey, MergedLangTable> tables =
                merge(List.of(root), extra("tfg", "lang/en_us/quests/a.json", Map.of()));

        assertEquals("Q", value(tables, "tfg", "en_us", "item.tfg.quest"));
    }

    @Test
    void laterLexicographicFragmentWinsAndSkipsMetadata(@TempDir Path temp) throws Exception {
        Path root = packWithAssets(temp, "tfg");
        write(root.resolve("assets/tfg/lang/en_us/a.json"), "{\"item.tfg.x\":\"A\",\"__x\":\"no\"}");
        write(root.resolve("assets/tfg/lang/en_us/b.json"), "{\"item.tfg.x\":\"B\"}");

        Map<LocaleKey, MergedLangTable> tables = merge(List.of(root), Extra.NONE);

        LocaleKey key = new LocaleKey("tfg", "en_us");
        assertEquals("B", tables.get(key).entries().get("item.tfg.x"));
        assertFalse(tables.get(key).entries().containsKey("__x"));
    }

    @Test
    void zipRootFragmentsMergeWithoutExtra(@TempDir Path temp) throws Exception {
        Path jar = temp.resolve("mod.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
            put(zip, "assets/tfg/lang/en_us/x.json", "{\"item.tfg.x\":\"jar\"}");
        }

        Map<LocaleKey, MergedLangTable> tables = merge(List.of(jar), Extra.NONE);

        assertEquals("jar", value(tables, "tfg", "en_us", "item.tfg.x"));
    }

    @Test
    void nonZipJarFileDoesNotCrash(@TempDir Path temp) throws Exception {
        Path fakeJar = temp.resolve("broken.jar");
        Files.writeString(fakeJar, "not a zip", StandardCharsets.UTF_8);

        Map<LocaleKey, MergedLangTable> tables = assertDoesNotThrow(() -> merge(List.of(fakeJar), Extra.NONE));

        assertTrue(tables.isEmpty());
    }

    @Test
    void emptyFragmentReadsYieldNoTable(@TempDir Path temp) throws Exception {
        Path root = packWithAssets(temp, "tfg");
        write(root.resolve("assets/tfg/lang/en_us/a.json"), "{}");

        Map<LocaleKey, MergedLangTable> tables = merge(List.of(root), Extra.NONE);

        assertFalse(tables.containsKey(new LocaleKey("tfg", "en_us")));
    }
}
