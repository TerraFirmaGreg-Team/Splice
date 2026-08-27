package team.terrafirmagrag.splice.util;

import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LangPaths {

  public String flatPath(String locale, String extension) {
    return "lang/" + locale + "." + extension;
  }

  public String fragmentFolderPrefix(String locale) {
    return "lang/" + locale + "/";
  }

  public String flatLocale(String path, String extension) {
    String suffix = "." + extension;
    if (!path.startsWith("lang/") || !path.endsWith(suffix)) {
      return null;
    }
    String middle = path.substring("lang/".length(), path.length() - suffix.length());
    if (middle.isEmpty() || middle.contains("/")) {
      return null;
    }
    return middle.toLowerCase(Locale.ROOT);
  }

  public String fragmentLocaleFromPath(String path) {
    if (!path.startsWith("lang/")) {
      return null;
    }
    int secondSlash = path.indexOf('/', "lang/".length());
    if (secondSlash < 0) {
      return null;
    }
    String locale = path.substring("lang/".length(), secondSlash);
    if (locale.isEmpty()) {
      return null;
    }
    return locale.toLowerCase(Locale.ROOT);
  }
}
