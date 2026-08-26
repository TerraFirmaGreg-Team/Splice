package team.terrafirmagrag.splice.walk;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LangFolderWalker {

  public List<Path> collectFragmentFiles(Path langRoot, String locale, String extension)
      throws IOException {
    Path folder = langRoot.resolve(locale);
    if (!Files.isDirectory(folder)) {
      return List.of();
    }
    String suffix = extension.startsWith(".") ? extension : "." + extension;
    List<Path> files = new ArrayList<>();
    Files.walkFileTree(
        folder,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(suffix)) {
              files.add(file);
            }
            return FileVisitResult.CONTINUE;
          }
        });
    files.sort(Comparator.comparing(Path::toString));
    return List.copyOf(files);
  }

  public List<String> filterResourcePaths(Iterable<String> paths, String prefix, String extension) {
    String suffix = extension.startsWith(".") ? extension : "." + extension;
    List<String> matched = new ArrayList<>();
    for (String path : paths) {
      if (path.startsWith(prefix) && path.endsWith(suffix)) {
        matched.add(path);
      }
    }
    matched.sort(String::compareTo);
    return List.copyOf(matched);
  }
}
