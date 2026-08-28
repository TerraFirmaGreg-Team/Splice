package team.terrafirmagrag.splice.model;

import java.util.Map;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface Extra {
  Extra NONE = (namespace, path) -> Map.of();

  Map<String, String> get(String namespace, String path);

  default void extraPaths(BiConsumer<String, String> sink) {}
}
