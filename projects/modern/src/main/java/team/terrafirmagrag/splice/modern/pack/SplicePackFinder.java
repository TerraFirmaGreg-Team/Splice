package team.terrafirmagrag.splice.modern.pack;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.terrafirmagrag.splice.modern.SpliceMod;
import team.terrafirmagrag.splice.modern.reload.LangCacheBuilder;

@Mod.EventBusSubscriber(
    modid = SpliceMod.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class SplicePackFinder {

  private SplicePackFinder() {}

  @SubscribeEvent
  public static void onAddPackFinders(AddPackFindersEvent event) {
    if (event.getPackType() != PackType.CLIENT_RESOURCES) {
      return;
    }
    Component description = Component.literal("Splice Merged Lang");
    Pack.Info info =
        new Pack.Info(
            description,
            SharedConstants.RESOURCE_PACK_FORMAT,
            SharedConstants.RESOURCE_PACK_FORMAT,
            FeatureFlagSet.of(),
            true);
    event.addRepositorySource(
        consumer ->
            consumer.accept(
                Pack.create(
                    SplicePackResources.PACK_ID,
                    description,
                    true,
                    SplicePackResources::new,
                    info,
                    event.getPackType(),
                    Pack.Position.TOP,
                    true,
                    PackSource.BUILT_IN)));
  }

  @SubscribeEvent
  public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(new LangCacheBuilder(SpliceMod.CACHE));
  }
}
