package team.terrafirmagrag.splice.vintage.pack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.resource.IResourceType;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.client.resource.VanillaResourceType;
import team.terrafirmagrag.splice.format.PropertiesLangFormat;
import team.terrafirmagrag.splice.model.MergedLangCache;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.vintage.reload.LangCacheBuilder;

public final class SpliceResourcePack implements IResourcePack, ISelectiveResourceReloadListener {

    public static final String PACK_NAME = "Splice Merged Lang";

    private final MergedLangCache cache;
    private final LangCacheBuilder builder;

    public SpliceResourcePack(MergedLangCache cache) {
        this.cache = cache;
        this.builder = new LangCacheBuilder(cache);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager, Predicate<IResourceType> resourcePredicate) {
        if (!resourcePredicate.test(VanillaResourceType.LANGUAGES)) {
            return;
        }
        if (resourceManager instanceof SimpleReloadableResourceManager reloadable) {
            reloadable.reloadResourcePack(this);
        }
        builder.rebuild(resourceManager);
        Minecraft.getMinecraft().getLanguageManager().onResourceManagerReload(resourceManager);
    }

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        MergedLangTable table = cache.tableFor(location.getNamespace(), location.getPath(), "lang");
        if (table == null || table.isEmpty()) {
            throw new IOException("No merged lang for " + location);
        }
        return new ByteArrayInputStream(PropertiesLangFormat.write(table.entries()));
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        if (builder.isRebuilding()) {
            return false;
        }
        MergedLangTable table = cache.tableFor(location.getNamespace(), location.getPath(), "lang");
        return table != null && !table.isEmpty();
    }

    @Override
    public Set<String> getResourceDomains() {
        return cache.namespaces();
    }

    @Override
    public String getPackName() {
        return PACK_NAME;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public <T extends IMetadataSection> T getPackMetadata(
            MetadataSerializer metadataSerializer, String metadataSectionName) {
        return null;
    }
}
