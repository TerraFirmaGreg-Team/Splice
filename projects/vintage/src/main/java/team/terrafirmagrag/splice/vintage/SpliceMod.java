package team.terrafirmagrag.splice.vintage;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.terrafirmagrag.splice.model.MergedLangCache;

@Mod(
    modid = SpliceMod.MOD_ID,
    name = "Splice",
    version = "0.1.0",
    clientSideOnly = true,
    acceptableRemoteVersions = "*")
public final class SpliceMod {
  public static final String MOD_ID = "splice";
  public static final MergedLangCache CACHE = new MergedLangCache();
  public static final Logger LOGGER = LogManager.getLogger("Splice");

  public SpliceMod() {
    LOGGER.info("Splice vintage loaded");
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    MinecraftForge.EVENT_BUS.register(SpliceClientBootstrap.class);
  }
}
