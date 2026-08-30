package team.terrafirmagrag.splice.modern.reload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import team.terrafirmagrag.splice.SpliceLog;
import team.terrafirmagrag.splice.format.JsonLangFormat;
import team.terrafirmagrag.splice.merge.LangFragmentMerger;
import team.terrafirmagrag.splice.merge.LangMergePipeline;
import team.terrafirmagrag.splice.model.*;
import team.terrafirmagrag.splice.modern.pack.SplicePackResources;

@RequiredArgsConstructor
public final class LangCacheBuilder implements PreparableReloadListener {

    private final MergedLangCache cache;
    private final MergePolicy policy = MergePolicy.withLogger();

    private static MultiPackResourceManager multiPack(ResourceManager manager) {
        if (manager instanceof MultiPackResourceManager multi) {
            return multi;
        }
        if (manager instanceof ReloadableResourceManager reloadable
                && reloadable.resources instanceof MultiPackResourceManager multi) {
            return multi;
        }
        return null;
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
                            attachMergedPack(manager);
                            List<String> keys = tables.keySet().stream()
                                    .map(key -> key.namespace() + "/" + key.localeCode())
                                    .sorted()
                                    .toList();
                            SpliceLog.log.info(
                                    "Splice merged {} namespace/locale lang table(s): {}", tables.size(), keys);
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.getLanguageManager() != null) {
                                mc.getLanguageManager().onResourceManagerReload(manager);
                            }
                        },
                        gameExecutor);
    }

    private Map<LocaleKey, MergedLangTable> build(ResourceManager manager) {
        Map<LocaleKey, MergedLangTable> tables =
                LangMergePipeline.merge(packFolders(manager), extra(manager), "json", policy);
        Set<String> known = manager.getNamespaces();
        tables.keySet().removeIf(key -> !known.contains(key.namespace()));
        return tables;
    }

    private Extra extra(ResourceManager manager) {
        return new Extra() {
            @Override
            public Map<String, String> get(String namespace, String path) {
                ResourceLocation id = ResourceLocation.tryBuild(namespace, path);
                return id == null ? Map.of() : mergeResourceStack(id, manager.getResourceStack(id));
            }

            @Override
            public void extraPaths(BiConsumer<String, String> sink) {
                manager.listResourceStacks("lang", location -> true)
                        .keySet()
                        .forEach(location -> sink.accept(
                                location.getNamespace(), location.getPath().toLowerCase(Locale.ROOT)));
            }
        };
    }

    private Map<String, String> mergeResourceStack(ResourceLocation id, List<Resource> stack) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (Resource resource : stack) {
            if (SplicePackResources.PACK_ID.equals(resource.sourcePackId())) {
                continue;
            }
            try (var in = resource.open()) {
                Map<String, String> parsed = JsonLangFormat.parse(in, JsonLangFormat.nestedWarningLogger());
                LangFragmentMerger.mergeInto(policy, merged, parsed);
            } catch (Exception e) {
                SpliceLog.log.warn("Failed to read lang resource {}", id, e);
            }
        }
        return merged;
    }

    private List<Path> packFolders(ResourceManager manager) {
        LinkedHashSet<Path> folders = new LinkedHashSet<>();
        for (PackResources pack : manager.listPacks().toList()) {
            if (SplicePackResources.PACK_ID.equals(pack.packId())) {
                continue;
            }
            Path directory = directoryRoot(pack);
            if (directory != null) {
                folders.add(directory);
            }
        }
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        try (var stream = Files.newDirectoryStream(gameDir)) {
            for (Path child : stream) {
                if (Files.isDirectory(child.resolve("assets"))) {
                    folders.add(child);
                }
            }
        } catch (IOException e) {
            SpliceLog.log.debug("Could not scan game dir for asset folders", e);
        }
        return List.copyOf(folders);
    }

    private void attachMergedPack(ResourceManager manager) {
        PackResources splice = manager.listPacks()
                .filter(pack -> SplicePackResources.PACK_ID.equals(pack.packId()))
                .findFirst()
                .orElse(null);
        MultiPackResourceManager multi = multiPack(manager);
        if (splice == null || multi == null) {
            return;
        }
        for (String namespace : cache.namespaces()) {
            FallbackResourceManager fallback = multi.namespacedManagers.get(namespace);
            if (fallback != null) {
                fallback.push(splice);
            }
        }
    }

    private Path directoryRoot(PackResources pack) {
        if (pack instanceof PathPackResources vanilla) {
            return vanilla.root;
        }
        if (pack instanceof net.minecraftforge.resource.PathPackResources forge) {
            return forge.getSource();
        }
        return null;
    }
}
