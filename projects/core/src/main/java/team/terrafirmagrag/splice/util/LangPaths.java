package team.terrafirmagrag.splice.util;

import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LangPaths {

  public String flatJsonPath(String locale) {
    return "lang/" + locale + ".json";
  }

  public String flatLangPath(String locale) {
    return "lang/" + locale + ".lang";
  }

  public String fragmentFolderPrefix(String locale) {
    return "lang/" + locale + "/";
  }

  public String flatLocaleFromJsonPath(String path) {
    if (!path.startsWith("lang/") || !path.endsWith(".json")) {
      return null;
    }
    String middle = path.substring("lang/".length(), path.length() - ".json".length());
    if (middle.isEmpty() || middle.contains("/")) {
      return null;
    }
    return middle.toLowerCase(Locale.ROOT);
  }

  public String flatLocaleFromLangPath(String path) {
    if (!path.startsWith("lang/") || !path.endsWith(".lang")) {
      return null;
    }
    String middle = path.substring("lang/".length(), path.length() - ".lang".length());
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
