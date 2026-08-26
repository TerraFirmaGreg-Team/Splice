package team.terrafirmagrag.splice.merge;

import java.util.function.BiConsumer;

public record MergePolicy(
    boolean skipMetadataKeys, BiConsumer<String, String> onDuplicateOverride) {

  public static MergePolicy defaults() {
    return new MergePolicy(true, (key, message) -> {});
  }

  public static MergePolicy withLogger(org.apache.logging.log4j.Logger logger) {
    return new MergePolicy(
        true,
        (key, message) ->
            logger.warn("Lang key \"{}\" overwritten during merge: {}", key, message));
  }

  public boolean shouldSkipKey(String key) {
    return skipMetadataKeys && key.startsWith("__");
  }
}
