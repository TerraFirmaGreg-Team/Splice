package team.terrafirmagrag.splice.vintage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.terrafirmagrag.splice.vintage.pack.SpliceResourcePack;

public final class SpliceClientBootstrap {

  private static boolean registered;

  private SpliceClientBootstrap() {}

  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  public static void onClientTick(TickEvent.ClientTickEvent event) {
    if (registered || event.phase != TickEvent.Phase.END) {
      return;
    }
    Minecraft mc = Minecraft.getMinecraft();
    if (!(mc.getResourceManager() instanceof SimpleReloadableResourceManager reloadable)) {
      return;
    }
    SpliceResourcePack pack = new SpliceResourcePack(SpliceMod.CACHE);
    reloadable.registerReloadListener(pack);
    reloadable.reloadResourcePack(pack);
    registered = true;
    SpliceMod.LOGGER.info("Splice vintage resource pack registered");
  }
}
