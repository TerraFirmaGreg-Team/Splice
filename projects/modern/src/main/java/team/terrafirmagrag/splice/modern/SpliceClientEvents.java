package team.terrafirmagrag.splice.modern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.terrafirmagrag.splice.modern.reload.LangCacheBuilder;

@Mod.EventBusSubscriber(
    modid = SpliceMod.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class SpliceClientEvents {

  private SpliceClientEvents() {}

  @SubscribeEvent
  public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(new LangCacheBuilder(SpliceMod.CACHE));
  }
}
