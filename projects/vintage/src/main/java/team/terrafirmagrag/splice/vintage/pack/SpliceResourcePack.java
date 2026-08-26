package team.terrafirmagrag.splice.vintage.pack;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import team.terrafirmagrag.splice.format.PropertiesLangFormat;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergedLangCache;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.util.LangPaths;
import team.terrafirmagrag.splice.vintage.reload.LangCacheBuilder;

public final class SpliceResourcePack implements IResourcePack, IResourceManagerReloadListener {

  private final MergedLangCache cache;
  private final LangCacheBuilder builder;

  public SpliceResourcePack(MergedLangCache cache) {
    this.cache = cache;
    this.builder = new LangCacheBuilder(cache);
  }

  @Override
  public void onResourceManagerReload(IResourceManager resourceManager) {
    builder.rebuild(resourceManager);
  }

  @Override
  public InputStream getInputStream(ResourceLocation location) throws IOException {
    String locale = LangPaths.flatLocaleFromLangPath(location.getPath());
    if (locale == null) {
      throw new IOException("Not a merged lang resource: " + location);
    }
    MergedLangTable table = cache.get(new LocaleKey(location.getNamespace(), locale));
    if (table == null || table.isEmpty()) {
      throw new IOException("No merged lang for " + location);
    }
    return new ByteArrayInputStream(PropertiesLangFormat.write(table.entries()));
  }

  @Override
  public boolean resourceExists(ResourceLocation location) {
    String locale = LangPaths.flatLocaleFromLangPath(location.getPath());
    if (locale == null) {
      return false;
    }
    MergedLangTable table = cache.get(new LocaleKey(location.getNamespace(), locale));
    return table != null && !table.isEmpty();
  }

  @Override
  public Set<String> getResourceDomains() {
    return cache.snapshot().keySet().stream()
        .map(LocaleKey::namespace)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public String getPackName() {
    return "Splice Merged Lang";
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
