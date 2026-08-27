package team.terrafirmagrag.splice.vintage.reload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.Logger;
import team.terrafirmagrag.splice.format.PropertiesLangFormat;
import team.terrafirmagrag.splice.merge.LangFragmentMerger;
import team.terrafirmagrag.splice.merge.MergePolicy;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergedLangCache;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.util.LangPaths;
import team.terrafirmagrag.splice.vintage.SpliceMod;
import team.terrafirmagrag.splice.vintage.pack.SpliceResourcePack;
import team.terrafirmagrag.splice.walk.LangFolderWalker;

public final class LangCacheBuilder {

  private final MergedLangCache cache;
  private final Logger logger;
  private boolean rebuilding;

  public LangCacheBuilder(MergedLangCache cache) {
    this.cache = cache;
    this.logger = SpliceMod.LOGGER;
  }

  public boolean isRebuilding() {
    return rebuilding;
  }

  public void rebuild(IResourceManager manager) {
    if (rebuilding) {
      return;
    }
    rebuilding = true;
    try {
      MergePolicy policy = MergePolicy.withLogger(logger);
      Map<LocaleKey, MergedLangTable> out = new HashMap<>();
      Set<String> namespaces = new HashSet<>();
      namespaces.addAll(manager.getResourceDomains());
      namespaces.addAll(scanDiskNamespaces());
      for (String namespace : namespaces) {
        for (String locale : discoverLocales(namespace)) {
          LocaleKey key = new LocaleKey(namespace, locale);
          Map<String, String> merged = mergeLocale(manager, namespace, locale, policy);
          if (!merged.isEmpty()) {
            out.put(key, new MergedLangTable(merged));
          }
        }
      }
      cache.replace(out);
      logger.info("Splice vintage merged {} namespace/locale lang table(s)", out.size());
    } finally {
      rebuilding = false;
    }
  }

  private Set<String> discoverLocales(String namespace) {
    Set<String> locales = new HashSet<>();
    for (String path : collectAssetPaths(namespace)) {
      String flatLocale = LangPaths.flatLocale(path, "lang");
      if (flatLocale != null) {
        locales.add(flatLocale);
        continue;
      }
      String fragmentLocale = LangPaths.fragmentLocaleFromPath(path);
      if (fragmentLocale != null) {
        locales.add(fragmentLocale);
      }
    }
    return locales;
  }

  private Map<String, String> mergeLocale(
      IResourceManager manager, String namespace, String locale, MergePolicy policy) {
    ResourceLocation flatId = new ResourceLocation(namespace, LangPaths.flatPath(locale, "lang"));
    Map<String, String> flatLayer = mergeResourceStack(manager, flatId, policy);

    String fragmentPrefix = LangPaths.fragmentFolderPrefix(locale);
    List<String> fragmentPaths =
        LangFolderWalker.filterResourcePaths(collectAssetPaths(namespace), fragmentPrefix, ".lang");

    List<Map<String, String>> fragments = new ArrayList<>();
    for (String path : fragmentPaths) {
      ResourceLocation fragmentId = new ResourceLocation(namespace, path);
      Map<String, String> fragmentLayer = mergeResourceStack(manager, fragmentId, policy);
      if (fragmentLayer.isEmpty()) {
        fragmentLayer = readDiskFragment(namespace, path, policy);
      }
      if (!fragmentLayer.isEmpty()) {
        fragments.add(fragmentLayer);
      }
    }

    return LangFragmentMerger.merge(
        policy, flatLayer.isEmpty() ? Optional.empty() : Optional.of(flatLayer), fragments);
  }

  private Map<String, String> mergeResourceStack(
      IResourceManager manager, ResourceLocation id, MergePolicy policy) {
    Map<String, String> merged = new LinkedHashMap<>();
    try {
      for (IResource resource : manager.getAllResources(id)) {
        if (SpliceResourcePack.PACK_NAME.equals(resource.getResourcePackName())) {
          continue;
        }
        try (InputStream in = resource.getInputStream()) {
          LangFragmentMerger.mergeInto(policy, merged, PropertiesLangFormat.parse(in));
        } catch (IOException e) {
          logger.warn(
              "Failed to read lang resource {} from {}", id, resource.getResourcePackName(), e);
        }
      }
    } catch (IOException e) {
      logger.debug("No resource stack for {}", id);
    }
    return merged;
  }

  private Map<String, String> readDiskFragment(String namespace, String path, MergePolicy policy) {
    Path groovy = gameDir().resolve("groovy/assets").resolve(namespace).resolve(path);
    if (!Files.isRegularFile(groovy)) {
      return Map.of();
    }
    try {
      Map<String, String> parsed = PropertiesLangFormat.parseFile(groovy);
      Map<String, String> filtered = new LinkedHashMap<>();
      LangFragmentMerger.mergeInto(policy, filtered, parsed);
      return filtered;
    } catch (IOException e) {
      logger.warn("Failed to read groovy lang fragment {}", groovy, e);
      return Map.of();
    }
  }

  public Set<String> discoverNamespaces() {
    Set<String> namespaces = new HashSet<>();
    for (ModContainer container : Loader.instance().getActiveModList()) {
      namespaces.add(container.getModId());
    }
    namespaces.addAll(scanDiskNamespaces());
    cache.snapshot().keySet().forEach(key -> namespaces.add(key.namespace()));
    return namespaces;
  }

  private Set<String> scanDiskNamespaces() {
    Set<String> namespaces = new HashSet<>();
    Path groovyAssets = gameDir().resolve("groovy/assets");
    if (!Files.isDirectory(groovyAssets)) {
      return namespaces;
    }
    try (var stream = Files.newDirectoryStream(groovyAssets)) {
      for (Path ns : stream) {
        if (Files.isDirectory(ns)) {
          namespaces.add(ns.getFileName().toString());
        }
      }
    } catch (IOException e) {
      logger.warn("Failed to scan groovy/assets namespaces", e);
    }
    return namespaces;
  }

  private List<String> collectAssetPaths(String namespace) {
    List<String> paths = new ArrayList<>();
    String prefix = "assets/" + namespace + "/";
    for (ModContainer container : Loader.instance().getActiveModList()) {
      File source = container.getSource();
      if (source == null || !source.isFile()) {
        continue;
      }
      try (ZipFile zip = new ZipFile(source)) {
        zip.stream()
            .map(ZipEntry::getName)
            .filter(name -> name.startsWith(prefix) && !name.endsWith("/"))
            .map(name -> name.substring(prefix.length()))
            .filter(name -> name.startsWith("lang/"))
            .forEach(paths::add);
      } catch (IOException e) {
        logger.debug("Could not scan jar {} for lang paths", source, e);
      }
    }
    Path groovyNs = gameDir().resolve("groovy/assets").resolve(namespace);
    Path groovyLang = groovyNs.resolve("lang");
    if (Files.isDirectory(groovyLang)) {
      try (var walk = Files.walk(groovyLang)) {
        walk.filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".lang"))
            .forEach(file -> paths.add(groovyNs.relativize(file).toString().replace('\\', '/')));
      } catch (IOException e) {
        logger.warn("Failed to walk groovy lang folder for {}", namespace, e);
      }
    }
    return paths;
  }

  private static Path gameDir() {
    return Loader.instance().getConfigDir().toPath().getParent();
  }
}
