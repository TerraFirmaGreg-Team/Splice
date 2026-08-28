package team.terrafirmagrag.splice.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import team.terrafirmagrag.splice.model.Extra;
import team.terrafirmagrag.splice.model.LangSource;

class LangIndexTest {

  @Test
  void indexesAssetsAndPackRootLang(@TempDir Path temp) throws Exception {
    Path assetsPack = temp.resolve("assetsPack");
    Path quests = assetsPack.resolve("assets/tfg/lang/en_us/Quests/a.json");
    Files.createDirectories(quests.getParent());
    Files.writeString(quests, "{}", StandardCharsets.UTF_8);

    Path folderPack = temp.resolve("folderPack");
    Path lang = folderPack.resolve("mymod/lang");
    Files.createDirectories(lang.resolve("en_us"));
    Files.writeString(lang.resolve("en_us.lang"), "a=1", StandardCharsets.UTF_8);
    Files.writeString(lang.resolve("en_us/items.lang"), "b=2", StandardCharsets.UTF_8);

    LangIndex json = LangIndex.of(List.of(assetsPack), Extra.NONE, ".json");
    assertEquals(Set.of("lang/en_us/quests/a.json"), json.paths("tfg"));
    assertEquals(quests, json.source("tfg", "lang/en_us/quests/a.json").file());

    LangIndex langs = LangIndex.of(List.of(folderPack), Extra.NONE, ".lang");
    assertEquals(
        List.of("lang/en_us.lang", "lang/en_us/items.lang"), List.copyOf(langs.paths("mymod")));
  }

  @Test
  void indexesZipAndExtraPaths(@TempDir Path temp) throws Exception {
    Path jar = temp.resolve("mod.jar");
    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
      zip.putNextEntry(new ZipEntry("assets/tfg/lang/en_us/x.json"));
      zip.write("{}".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
      zip.putNextEntry(new ZipEntry("assets/tfg/textures/x.png"));
      zip.write("nope".getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }

    LangIndex zipped = LangIndex.of(List.of(jar), Extra.NONE, ".json");
    LangSource source = zipped.source("tfg", "lang/en_us/x.json");
    assertEquals(Set.of("lang/en_us/x.json"), zipped.paths("tfg"));
    assertEquals(jar, source.zip());
    assertEquals("assets/tfg/lang/en_us/x.json", source.zipEntry());

    Extra extra =
        new Extra() {
          @Override
          public Map<String, String> get(String namespace, String path) {
            return Map.of();
          }

          @Override
          public void extraPaths(BiConsumer<String, String> sink) {
            sink.accept("tfg", "lang/en_us/a.json");
          }
        };
    LangIndex fromExtra = LangIndex.of(List.of(), extra, ".json");
    assertEquals(Set.of("lang/en_us/a.json"), fromExtra.paths("tfg"));
    assertNull(fromExtra.source("tfg", "lang/en_us/a.json"));
  }

  @Test
  void firstRootWinsForSameCanonicalPath(@TempDir Path temp) throws Exception {
    Path first = temp.resolve("first");
    Path second = temp.resolve("second");
    Path a = first.resolve("assets/tfg/lang/en_us/a.json");
    Path b = second.resolve("assets/tfg/lang/en_us/a.json");
    Files.createDirectories(a.getParent());
    Files.createDirectories(b.getParent());
    Files.writeString(a, "{}", StandardCharsets.UTF_8);
    Files.writeString(b, "{}", StandardCharsets.UTF_8);

    LangIndex index = LangIndex.of(List.of(first, second), Extra.NONE, ".json");

    assertEquals(a, index.source("tfg", "lang/en_us/a.json").file());
  }
}
