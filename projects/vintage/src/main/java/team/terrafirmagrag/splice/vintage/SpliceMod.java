package team.terrafirmagrag.splice.vintage;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import team.terrafirmagrag.splice.SpliceLog;
import team.terrafirmagrag.splice.model.MergedLangCache;

@Mod(modid = SpliceMod.MOD_ID, name = "Splice", clientSideOnly = true, acceptableRemoteVersions = "*")
public final class SpliceMod {
    public static final String MOD_ID = "splice";
    public static final MergedLangCache CACHE = new MergedLangCache();

    public SpliceMod() {
        SpliceLog.log.info("Splice vintage loaded");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {

        MinecraftForge.EVENT_BUS.register(SpliceClientBootstrap.class);
    }
}
