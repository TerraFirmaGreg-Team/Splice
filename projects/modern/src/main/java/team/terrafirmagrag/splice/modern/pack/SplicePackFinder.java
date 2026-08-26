package team.terrafirmagrag.splice.modern.pack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.terrafirmagrag.splice.modern.SpliceMod;

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
    event.addRepositorySource(
        consumer ->
            consumer.accept(
                Pack.readMetaAndCreate(
                    "splice_merged",
                    Component.literal("Splice Merged Lang"),
                    false,
                    SplicePackResources::new,
                    event.getPackType(),
                    Pack.Position.TOP,
                    PackSource.BUILT_IN)));
  }
}
