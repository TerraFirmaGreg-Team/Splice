package team.terrafirmagrag.splice.vintage.reload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import team.terrafirmagrag.splice.SpliceLog;
import team.terrafirmagrag.splice.format.PropertiesLangFormat;
import team.terrafirmagrag.splice.merge.LangFragmentMerger;
import team.terrafirmagrag.splice.merge.LangMergePipeline;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergePolicy;
import team.terrafirmagrag.splice.model.MergedLangCache;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.vintage.pack.SpliceResourcePack;

@RequiredArgsConstructor
public final class LangCacheBuilder {

    private final MergedLangCache cache;

    @Getter
    private boolean rebuilding;

    public void rebuild(IResourceManager manager) {
        if (rebuilding) {
            return;
        }
        rebuilding = true;
        try {
            MergePolicy policy = MergePolicy.withLogger();
            Map<LocaleKey, MergedLangTable> out = LangMergePipeline.merge(
                    packRoots(),
                    (namespace, path) -> mergeResourceStack(manager, new ResourceLocation(namespace, path), policy),
                    "lang",
                    policy);
            Set<String> known = manager.getResourceDomains();
            out.keySet().removeIf(key -> !known.contains(key.namespace()));
            cache.replace(out);
            SpliceLog.log.info("Splice vintage merged {} namespace/locale lang table(s)", out.size());
        } finally {
            rebuilding = false;
        }
    }

    private Map<String, String> mergeResourceStack(IResourceManager manager, ResourceLocation id, MergePolicy policy) {
        Map<String, String> merged = new LinkedHashMap<>();
        try {
            for (IResource resource : manager.getAllResources(id)) {
                if (SpliceResourcePack.PACK_NAME.equals(resource.getResourcePackName())) {
                    continue;
                }
                try (InputStream in = resource.getInputStream()) {
                    LangFragmentMerger.mergeInto(policy, merged, PropertiesLangFormat.parse(in));
                } catch (IOException e) {
                    SpliceLog.log.warn(
                            "Failed to read lang resource {} from {}", id, resource.getResourcePackName(), e);
                }
            }
        } catch (IOException e) {
            SpliceLog.log.debug("No resource stack for {}", id);
        }
        return merged;
    }

    private List<Path> packRoots() {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        for (ModContainer container : Loader.instance().getActiveModList()) {
            File source = container.getSource();
            if (source != null && source.isFile()) {
                roots.add(source.toPath());
            }
        }
        Minecraft mc = Minecraft.getMinecraft();
        List<IResourcePack> packs = new ArrayList<>(mc.defaultResourcePacks);
        for (ResourcePackRepository.Entry entry : mc.getResourcePackRepository().getRepositoryEntries()) {
            packs.add(entry.getResourcePack());
        }
        for (IResourcePack pack : packs) {
            if (pack instanceof FolderResourcePack folder) {
                roots.add(folder.resourcePackFile.toPath());
            }
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mc.gameDir.toPath())) {
            for (Path child : stream) {
                if (Files.isDirectory(child.resolve("assets"))) {
                    roots.add(child);
                }
            }
        } catch (IOException e) {
            SpliceLog.log.debug("Could not scan game dir for asset folders", e);
        }
        return List.copyOf(roots);
    }
}
