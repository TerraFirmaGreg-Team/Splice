package team.terrafirmagrag.splice.model;

import java.nio.file.Path;

public record LangSource(Path file, Path zip, String zipEntry) {}
