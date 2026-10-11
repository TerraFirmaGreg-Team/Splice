package team.terrafirmagrag.splice.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import team.terrafirmagrag.splice.SpliceLog;

public record MergePolicy(boolean skipMetadataKeys, BiConsumer<String, String> onDuplicateOverride) {

    public static MergePolicy defaults() {
        return new MergePolicy(true, (key, message) -> {});
    }

    public static MergePolicy withLogger(AtomicInteger overrideCount) {
        return new MergePolicy(true, (key, message) -> {
            overrideCount.incrementAndGet();
            SpliceLog.log.debug("Lang key \"{}\" overwritten during merge: {}", key, message);
        });
    }

    public boolean shouldSkipKey(String key) {
        return skipMetadataKeys && key.startsWith("__");
    }
}
