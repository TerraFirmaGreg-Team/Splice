package team.terrafirmagrag.splice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import team.terrafirmagrag.splice.model.JarLangEntry;

class LangPathsTest {

  @Test
  void filterResourcePathsKeepsPrefixedFilesInLexicographicOrder() {
    List<String> filtered =
        LangPaths.filterResourcePaths(
            List.of(
                "lang/en_us/items.json",
                "lang/en_us/blocks.json",
                "lang/en_us.json",
                "textures/block/stone.png"),
            "lang/en_us/",
            ".json");

    assertEquals(List.of("lang/en_us/blocks.json", "lang/en_us/items.json"), filtered);
  }

  @Test
  void fragmentLocalesFromIgnoresFlatFiles() {
    Set<String> locales =
        LangPaths.fragmentLocalesFrom(
            List.of(
                "lang/en_us.json",
                "lang/en_us/items.json",
                "lang/ru_ru.json",
                "lang/de_de/quests/tfg.json"));

    assertEquals(Set.of("en_us", "de_de"), locales);
  }

  @Test
  void langEntriesFromJarFindsLangPaths(@TempDir Path temp) throws Exception {
    Path jar = temp.resolve("mod.jar");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
      put(zip, "assets/mymod/lang/en_us.lang", "a=b");
      put(zip, "assets/mymod/lang/en_us/items.lang", "c=d");
      put(zip, "assets/mymod/textures/item.png", "nope");
      put(zip, "assets/other/lang/en_us.lang", "skip");
    }

    List<JarLangEntry> entries = LangPaths.langEntriesFromJar(jar.toFile());
    assertEquals(
        List.of("lang/en_us.lang", "lang/en_us/items.lang"),
        entries.stream()
            .filter(entry -> entry.namespace().equals("mymod"))
            .map(JarLangEntry::path)
            .sorted()
            .toList());
    assertEquals(
        List.of("mymod", "other"),
        entries.stream().map(JarLangEntry::namespace).distinct().sorted().toList());
  }

  private static void put(ZipOutputStream zip, String name, String content) throws Exception {
    zip.putNextEntry(new ZipEntry(name));
    zip.write(content.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }
}
