package team.terrafirmagrag.splice.modern;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import team.terrafirmagrag.splice.model.MergedLangCache;

@Mod(SpliceMod.MOD_ID)
public final class SpliceMod {
  public static final String MOD_ID = "splice";
  public static final MergedLangCache CACHE = new MergedLangCache();
  public static final Logger LOGGER = LogManager.getLogger("Splice");

  public SpliceMod() {
    if (FMLEnvironment.dist != Dist.CLIENT) {
      return;
    }
    LOGGER.info("Splice modern loaded");
  }
}
