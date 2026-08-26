package team.terrafirmagrag.splice.walk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LangFolderWalkerTest {

  @Test
  void collectsFragmentFilesInLexicographicOrder(@TempDir Path temp) throws Exception {
    Path langRoot = temp.resolve("lang");
    Path localeDir = langRoot.resolve("en_us");
    Files.createDirectories(localeDir.resolve("quests"));
    Files.writeString(localeDir.resolve("blocks.json"), "{}", StandardCharsets.UTF_8);
    Files.writeString(localeDir.resolve("items.json"), "{}", StandardCharsets.UTF_8);
    Files.writeString(localeDir.resolve("quests/tfg.json"), "{}", StandardCharsets.UTF_8);

    List<Path> files = LangFolderWalker.collectFragmentFiles(langRoot, "en_us", ".json");

    assertEquals(3, files.size());
    List<String> relative =
        files.stream().map(p -> langRoot.relativize(p).toString().replace('\\', '/')).toList();
    assertEquals(
        List.of("en_us/blocks.json", "en_us/items.json", "en_us/quests/tfg.json"), relative);
  }

  @Test
  void filtersResourcePaths() {
    List<String> paths =
        List.of(
            "lang/en_us/items.json",
            "lang/en_us/blocks.json",
            "lang/en_us.json",
            "textures/block/stone.png");

    List<String> filtered = LangFolderWalker.filterResourcePaths(paths, "lang/en_us/", ".json");

    assertEquals(List.of("lang/en_us/blocks.json", "lang/en_us/items.json"), filtered);
  }
}
