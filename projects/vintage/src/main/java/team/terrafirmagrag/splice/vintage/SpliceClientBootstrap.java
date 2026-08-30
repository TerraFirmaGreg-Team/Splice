package team.terrafirmagrag.splice.vintage;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import team.terrafirmagrag.splice.SpliceLog;
import team.terrafirmagrag.splice.vintage.pack.SpliceResourcePack;

@UtilityClass
public final class SpliceClientBootstrap {

    private static boolean registered;

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
        registered = true;
        FMLClientHandler.instance().refreshResources(VanillaResourceType.LANGUAGES);
        SpliceLog.log.info("Splice vintage resource pack registered");
    }
}
