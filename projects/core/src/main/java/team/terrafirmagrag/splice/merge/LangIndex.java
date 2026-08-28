package team.terrafirmagrag.splice.merge;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import team.terrafirmagrag.splice.model.Extra;
import team.terrafirmagrag.splice.model.JarLangEntry;
import team.terrafirmagrag.splice.model.LangSource;
import team.terrafirmagrag.splice.util.LangPaths;

public final class LangIndex {

  private final Map<String, LinkedHashSet<String>> pathsByNs = new HashMap<>();
  private final Map<String, Map<String, LangSource>> sourcesByNs = new HashMap<>();

  public static LangIndex of(Iterable<Path> packRoots, Extra extra, String suffix) {
    LangIndex index = new LangIndex();
    for (Path root : packRoots) {
      if (Files.isDirectory(root)) {
        index.indexDirectory(root, suffix);
      } else if (Files.isRegularFile(root)) {
        index.indexZip(root, suffix);
      }
    }
    extra.extraPaths(index::addPath);
    return index;
  }

  public Set<String> namespaces() {
    return pathsByNs.keySet();
  }

  public Set<String> paths(String namespace) {
    LinkedHashSet<String> paths = pathsByNs.get(namespace);
    return paths == null ? Set.of() : paths;
  }

  public LangSource source(String namespace, String path) {
    Map<String, LangSource> sources = sourcesByNs.get(namespace);
    return sources == null ? null : sources.get(path);
  }

  private void indexDirectory(Path packRoot, String suffix) {
    Path assets = packRoot.resolve("assets");
    if (Files.isDirectory(assets)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(assets)) {
        for (Path nsDir : stream) {
          if (Files.isDirectory(nsDir)) {
            indexNsRoot(nsDir.getFileName().toString(), nsDir, suffix);
          }
        }
      } catch (IOException ignored) {
      }
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(packRoot)) {
      for (Path child : stream) {
        if (Files.isDirectory(child)
            && !"assets".equals(child.getFileName().toString())
            && Files.isDirectory(child.resolve("lang"))) {
          indexNsRoot(child.getFileName().toString(), child, suffix);
        }
      }
    } catch (IOException ignored) {
    }
  }

  private void indexNsRoot(String namespace, Path nsRoot, String suffix) {
    try {
      for (String rel : langResourcePaths(nsRoot, suffix)) {
        addSource(namespace, rel, new LangSource(nsRoot.resolve(rel), null, null));
      }
    } catch (IOException ignored) {
    }
  }

  private void indexZip(Path zipPath, String suffix) {
    String lowerSuffix = suffix.toLowerCase(Locale.ROOT);
    try {
      for (JarLangEntry entry : LangPaths.langEntriesFromJar(zipPath.toFile())) {
        String canonical = entry.path().toLowerCase(Locale.ROOT);
        if (!canonical.endsWith(lowerSuffix)) {
          continue;
        }
        addSource(entry.namespace(), canonical, new LangSource(null, zipPath, entry.zipEntry()));
      }
    } catch (IOException ignored) {
    }
  }

  private void addPath(String namespace, String path) {
    pathsByNs
        .computeIfAbsent(namespace, key -> new LinkedHashSet<>())
        .add(path.toLowerCase(Locale.ROOT));
  }

  private void addSource(String namespace, String path, LangSource source) {
    String canonical = path.toLowerCase(Locale.ROOT);
    addPath(namespace, canonical);
    sourcesByNs.computeIfAbsent(namespace, key -> new HashMap<>()).putIfAbsent(canonical, source);
  }

  private static List<String> langResourcePaths(Path namespaceRoot, String suffix)
      throws IOException {
    Path lang = namespaceRoot.resolve("lang");
    if (!Files.isDirectory(lang)) {
      return List.of();
    }
    String lowerSuffix = suffix.toLowerCase(Locale.ROOT);
    try (Stream<Path> walk = Files.walk(lang)) {
      return walk.filter(Files::isRegularFile)
          .map(file -> namespaceRoot.relativize(file).toString().replace('\\', '/'))
          .filter(path -> path.toLowerCase(Locale.ROOT).endsWith(lowerSuffix))
          .sorted()
          .toList();
    }
  }
}
