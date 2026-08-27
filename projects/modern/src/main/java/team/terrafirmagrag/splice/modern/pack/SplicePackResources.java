package team.terrafirmagrag.splice.modern.pack;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.terrafirmagrag.splice.format.JsonLangFormat;
import team.terrafirmagrag.splice.model.LocaleKey;
import team.terrafirmagrag.splice.model.MergedLangTable;
import team.terrafirmagrag.splice.modern.SpliceMod;
import team.terrafirmagrag.splice.util.LangPaths;

public record SplicePackResources(String packId) implements PackResources {

  public static final String PACK_ID = "splice_merged";

  @Override
  public IoSupplier<InputStream> getRootResource(String... path) {
    return null;
  }

  @Override
  public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
    if (type != PackType.CLIENT_RESOURCES) {
      return null;
    }
    String locale = LangPaths.flatLocale(location.getPath(), "json");
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
    if (type != PackType.CLIENT_RESOURCES) {
      return;
    }
    if (!pathPrefix.isEmpty() && !pathPrefix.equals("lang") && !pathPrefix.startsWith("lang/")) {
      return;
    }
    for (LocaleKey key : SpliceMod.CACHE.snapshot().keySet()) {
      if (!key.namespace().equals(namespace)) {
        continue;
      }
      ResourceLocation id =
          ResourceLocation.fromNamespaceAndPath(
              namespace, LangPaths.flatPath(key.localeCode(), "json"));
      IoSupplier<InputStream> resource = getResource(type, id);
      if (resource != null) {
        output.accept(id, resource);
      }
    }
  }

  @Override
  public @NotNull Set<String> getNamespaces(PackType type) {
    if (type != PackType.CLIENT_RESOURCES) {
      return Set.of();
    }
    return discoverNamespaces();
  }

  private static Set<String> discoverNamespaces() {
    Set<String> namespaces = new HashSet<>();
    ModList.get().getMods().forEach(mod -> namespaces.add(mod.getModId()));
    Path kubejsAssets = FMLPaths.GAMEDIR.get().resolve("kubejs/assets");
    if (Files.isDirectory(kubejsAssets)) {
      try (var stream = Files.newDirectoryStream(kubejsAssets)) {
        for (Path ns : stream) {
          if (Files.isDirectory(ns)) {
            namespaces.add(ns.getFileName().toString());
          }
        }
      } catch (Exception e) {
        SpliceMod.LOGGER.debug("Failed to scan kubejs/assets namespaces", e);
      }
    }
    SpliceMod.CACHE.snapshot().keySet().forEach(key -> namespaces.add(key.namespace()));
    return Set.copyOf(namespaces);
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
    if (serializer == PackMetadataSection.TYPE) {
      return (T)
          new PackMetadataSection(
              Component.literal("Splice Merged Lang"), SharedConstants.RESOURCE_PACK_FORMAT);
    }
    return null;
  }

  @Override
  public @NotNull String packId() {
    return packId;
  }

  @Override
  public void close() {}
}
