package team.terrafirmagrag.splice.modern.reload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import team.terrafirmagrag.splice.format.JsonLangFormat;
import team.terrafirmagrag.splice.merge.LangFragmentMerger;
import team.terrafirmagrag.splice.merge.MergePolicy;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergedLangCache;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.modern.SpliceMod;
import team.terrafirmagrag.splice.modern.pack.SplicePackResources;
import team.terrafirmagrag.splice.util.LangPaths;

public final class LangCacheBuilder implements PreparableReloadListener {

  private final MergedLangCache cache;
  private final MergePolicy policy;

  public LangCacheBuilder(MergedLangCache cache) {
    this.cache = cache;
    this.policy = MergePolicy.withLogger(SpliceMod.LOGGER);
  }

  @Override
  public @NotNull CompletableFuture<Void> reload(
      PreparationBarrier barrier,
      ResourceManager manager,
      ProfilerFiller preparationProfiler,
      ProfilerFiller reloadProfiler,
      Executor backgroundExecutor,
      Executor gameExecutor) {
    return CompletableFuture.supplyAsync(() -> build(manager), backgroundExecutor)
        .thenCompose(barrier::wait)
        .thenAcceptAsync(
            tables -> {
              cache.replace(tables);
              SpliceMod.LOGGER.info(
                  "Splice merged {} namespace/locale lang table(s)", tables.size());
              Minecraft mc = Minecraft.getInstance();
              if (mc.getLanguageManager() != null) {
                mc.getLanguageManager().onResourceManagerReload(manager);
              }
            },
            gameExecutor);
  }

  private Map<LocaleKey, MergedLangTable> build(ResourceManager manager) {
    Map<LocaleKey, MergedLangTable> out = new HashMap<>();
    for (String namespace : manager.getNamespaces()) {
      for (String locale : discoverLocales(manager, namespace)) {
        LocaleKey key = new LocaleKey(namespace, locale);
        Map<String, String> merged = mergeLocale(manager, namespace, locale);
        if (!merged.isEmpty()) {
          out.put(key, new MergedLangTable(merged));
        }
      }
    }
    return out;
  }

  private Set<String> discoverLocales(ResourceManager manager, String namespace) {
    Set<String> locales = new HashSet<>();
    Map<ResourceLocation, List<Resource>> stacks =
        manager.listResourceStacks(
            "lang",
            loc ->
                loc.getNamespace().equals(namespace)
                    && (LangPaths.flatLocale(loc.getPath(), "json") != null
                        || LangPaths.fragmentLocaleFromPath(loc.getPath()) != null));
    for (ResourceLocation id : stacks.keySet()) {
      String path = id.getPath();
      String flatLocale = LangPaths.flatLocale(path, "json");
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
      ResourceManager manager, String namespace, String locale) {
    ResourceLocation flatId =
        ResourceLocation.fromNamespaceAndPath(namespace, LangPaths.flatPath(locale, "json"));
    Map<String, String> flatLayer = mergeResourceStack(manager, flatId);

    List<String> fragmentPaths =
        manager
            .listResourceStacks(
                "lang/" + locale,
                loc -> loc.getNamespace().equals(namespace) && loc.getPath().endsWith(".json"))
            .keySet()
            .stream()
            .map(ResourceLocation::getPath)
            .sorted()
            .toList();

    List<Map<String, String>> fragments = new ArrayList<>();
    for (String path : fragmentPaths) {
      ResourceLocation fragmentId = ResourceLocation.fromNamespaceAndPath(namespace, path);
      Map<String, String> fragmentLayer = mergeResourceStack(manager, fragmentId);
      if (!fragmentLayer.isEmpty()) {
        fragments.add(fragmentLayer);
      }
    }

    return LangFragmentMerger.merge(
        policy, flatLayer.isEmpty() ? Optional.empty() : Optional.of(flatLayer), fragments);
  }

  private Map<String, String> mergeResourceStack(ResourceManager manager, ResourceLocation id) {
    Map<String, String> merged = new LinkedHashMap<>();
    for (Resource resource : manager.getResourceStack(id)) {
      if (SplicePackResources.PACK_ID.equals(resource.sourcePackId())) {
        continue;
      }
      try (var in = resource.open()) {
        Map<String, String> parsed =
            JsonLangFormat.parse(in, JsonLangFormat.nestedWarningLogger(SpliceMod.LOGGER));
        LangFragmentMerger.mergeInto(policy, merged, parsed);
      } catch (Exception e) {
        SpliceMod.LOGGER.warn("Failed to read lang resource {}", id, e);
      }
    }
    return merged;
  }
}
