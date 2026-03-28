package com.agtext.knowledge.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StorageService {
  private final Path root;

  public StorageService(StorageProperties props) {
    this.root = Path.of(props.root() == null || props.root().isBlank() ? "./data" : props.root());
  }

  public String writeText(String relativeDir, String fileNameHint, String content) {
    try {
      Path dir = root.resolve(relativeDir);
      Files.createDirectories(dir);
      String safeName = sanitizeFileName(fileNameHint);
      String fileName =
          Instant.now().toEpochMilli()
              + "-"
              + safeName
              + "-"
              + UUID.randomUUID().toString().substring(0, 8)
              + ".txt";
      Path target = dir.resolve(fileName);
      Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
      return target.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file", e);
    }
  }

  public String writeBytes(String relativeDir, String fileNameHint, byte[] content, String ext) {
    try {
      Path dir = root.resolve(relativeDir);
      Files.createDirectories(dir);
      String safeName = sanitizeFileName(fileNameHint);
      String safeExt =
          ext == null || ext.isBlank() ? ".bin" : (ext.startsWith(".") ? ext : "." + ext);
      String fileName =
          Instant.now().toEpochMilli()
              + "-"
              + safeName
              + "-"
              + UUID.randomUUID().toString().substring(0, 8)
              + safeExt;
      Path target = dir.resolve(fileName);
      Files.write(target, content == null ? new byte[0] : content, StandardOpenOption.CREATE_NEW);
      return target.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file", e);
    }
  }

  public String readText(String absolutePath) {
    try {
      return Files.readString(Path.of(absolutePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + absolutePath, e);
    }
  }

  private static String sanitizeFileName(String name) {
    if (name == null || name.isBlank()) {
      return "content";
    }
    return name.replaceAll("[^a-zA-Z0-9._-]+", "_");
  }
}
