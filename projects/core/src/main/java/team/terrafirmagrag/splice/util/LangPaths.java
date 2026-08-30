package team.terrafirmagrag.splice.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import team.terrafirmagrag.splice.model.JarLangEntry;

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

    private String fragmentLocaleFromPath(String path) {
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

    public Set<String> fragmentLocalesFrom(Iterable<String> paths) {
        Set<String> locales = new HashSet<>();
        for (String path : paths) {
            String locale = fragmentLocaleFromPath(path);
            if (locale != null) {
                locales.add(locale);
            }
        }
        return locales;
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

    public List<JarLangEntry> langEntriesFromJar(File jar) throws IOException {
        List<JarLangEntry> entries = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar)) {
            zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .map(ZipEntry::getName)
                    .forEach(name -> {
                        JarLangEntry parsed = parseJarLangEntry(name);
                        if (parsed != null) {
                            entries.add(parsed);
                        }
                    });
        }
        return entries;
    }

    public byte[] readJarEntry(File jar, String zipEntry) throws IOException {
        try (ZipFile zip = new ZipFile(jar)) {
            ZipEntry entry = zip.getEntry(zipEntry);
            if (entry == null) {
                throw new IOException("missing zip entry " + zipEntry);
            }
            try (InputStream in = zip.getInputStream(entry)) {
                return in.readAllBytes();
            }
        }
    }

    private JarLangEntry parseJarLangEntry(String name) {
        String normalized = name.replace('\\', '/');
        if (!normalized.startsWith("assets/")) {
            return null;
        }
        int nsEnd = normalized.indexOf('/', "assets/".length());
        if (nsEnd < 0) {
            return null;
        }
        String rel = normalized.substring(nsEnd + 1);
        if (!rel.toLowerCase(Locale.ROOT).startsWith("lang/")) {
            return null;
        }
        return new JarLangEntry(normalized.substring("assets/".length(), nsEnd), rel, name);
    }
}
