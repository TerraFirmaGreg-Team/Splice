package team.terrafirmagrag.splice.modern.pack;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import team.terrafirmagrag.splice.format.JsonLangFormat;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.modern.SpliceMod;
import team.terrafirmagrag.splice.util.LangPaths;

@RequiredArgsConstructor
public final class SplicePackResources implements PackResources {

  private final String packId;

  @Override
  public IoSupplier<InputStream> getRootResource(String... path) {
    return null;
  }

  @Override
  public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
    if (type != PackType.CLIENT_RESOURCES) {
      return null;
    }
    String locale = LangPaths.flatLocaleFromJsonPath(location.getPath());
    if (locale == null) {
      return null;
    }
    MergedLangTable table = SpliceMod.CACHE.get(new LocaleKey(location.getNamespace(), locale));
    if (table == null || table.isEmpty()) {
      return null;
    }
    byte[] bytes = JsonLangFormat.write(table.entries());
    return () -> new ByteArrayInputStream(bytes);
  }

  @Override
  public void listResources(
      PackType type, String namespace, String pathPrefix, ResourceOutput output) {
    if (type != PackType.CLIENT_RESOURCES || !pathPrefix.isEmpty()) {
      return;
    }
    for (LocaleKey key : SpliceMod.CACHE.snapshot().keySet()) {
      if (!key.namespace().equals(namespace)) {
        continue;
      }
      ResourceLocation id =
          new ResourceLocation(namespace, LangPaths.flatJsonPath(key.localeCode()));
      IoSupplier<InputStream> resource = getResource(type, id);
      if (resource != null) {
        output.accept(id, resource);
      }
    }
  }

  @Override
  public Set<String> getNamespaces(PackType type) {
    if (type != PackType.CLIENT_RESOURCES) {
      return Set.of();
    }
    return SpliceMod.CACHE.snapshot().keySet().stream()
        .map(LocaleKey::namespace)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
    return null;
  }

  @Override
  public String packId() {
    return packId;
  }

  @Override
  public void close() {}
}
