package com.agtext.knowledge.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 存储服务：负责本地磁盘文件的 I/O 操作。
 * 提供文本和二进制数据的写入、读取，并自动处理文件名冲突与安全性。
 */
@Service
public class StorageService {
  private final Path root; // 存储根路径

  public StorageService(StorageProperties props) {
    // 构造时初始化根路径，若配置为空则默认使用当前目录下的 ./data
    this.root = Path.of(props.root() == null || props.root().isBlank() ? "./data" : props.root());
  }

  /**
   * 将文本内容写入文件
   * @param relativeDir 相对根目录的子目录路径（如 "knowledge/raw"）
   * @param fileNameHint 文件名提示（原始标题）
   * @param content 文本内容
   * @return 写入后的绝对路径字符串
   */
  public String writeText(String relativeDir, String fileNameHint, String content) {
    try {
      // 1. 确保目标目录存在
      Path dir = root.resolve(relativeDir);
      Files.createDirectories(dir);

      // 2. 清洗文件名并生成唯一的文件名
      // 格式：时间戳-安全文件名-随机UUID缩写.txt
      String safeName = sanitizeFileName(fileNameHint);
      String fileName =
              Instant.now().toEpochMilli()
                      + "-"
                      + safeName
                      + "-"
                      + UUID.randomUUID().toString().substring(0, 8)
                      + ".txt";

      Path target = dir.resolve(fileName);

      // 3. 写入文本，强制使用 UTF-8 编码
      Files.writeString(target, content == null ? "" : content, StandardCharsets.UTF_8);
      return target.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file", e);
    }
  }

  /**
   * 将二进制数据（字节数组）写入文件，常用于 PDF 存储
   * @param ext 文件扩展名（如 "pdf"）
   */
  public String writeBytes(String relativeDir, String fileNameHint, byte[] content, String ext) {
    try {
      Path dir = root.resolve(relativeDir);
      Files.createDirectories(dir);

      String safeName = sanitizeFileName(fileNameHint);
      // 处理扩展名格式，确保以 "." 开头
      String safeExt =
              ext == null || ext.isBlank() ? ".bin" : (ext.startsWith(".") ? ext : "." + ext);

      // 生成唯一文件名
      String fileName =
              Instant.now().toEpochMilli()
                      + "-"
                      + safeName
                      + "-"
                      + UUID.randomUUID().toString().substring(0, 8)
                      + safeExt;

      Path target = dir.resolve(fileName);

      // 4. 写入字节数据，CREATE_NEW 确保不会覆盖已有文件（理论上 UUID 已规避冲突）
      Files.write(target, content == null ? new byte[0] : content, StandardOpenOption.CREATE_NEW);
      return target.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write file", e);
    }
  }

  /**
   * 根据绝对路径读取文本文件内容
   */
  public String readText(String absolutePath) {
    try {
      return Files.readString(Path.of(absolutePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read file: " + absolutePath, e);
    }
  }

  /**
   * 文件名安全过滤：
   * 只允许字母、数字、点、下划线和连字符，防止目录遍历攻击（Path Traversal）或特殊字符导致的系统异常。
   */
  private static String sanitizeFileName(String name) {
    if (name == null || name.isBlank()) {
      return "content";
    }
    // 将不符合规则的字符替换为下划线
    return name.replaceAll("[^a-zA-Z0-9._-]+", "_");
  }
}