package com.famora.backup.service;

import com.famora.common.exception.AppException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BackupTempStorageCleanupService {

  private final Path root;

  public BackupTempStorageCleanupService(@Value("${app.backup.temp-root:}") String configuredRoot) {
    this.root = resolveRoot(configuredRoot);
  }

  public void delete(String storagePath) {
    try {
      Path path = Paths.get(storagePath).toAbsolutePath().normalize();
      if (!path.startsWith(root)) {
        throw new AppException(HttpStatus.FORBIDDEN, "Invalid backup temporary path");
      }
      Files.deleteIfExists(path);
    } catch (AppException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to delete backup temporary object");
    }
  }

  private Path resolveRoot(String configuredRoot) {
    if (StringUtils.hasText(configuredRoot)) {
      return Paths.get(configuredRoot).toAbsolutePath().normalize();
    }
    String systemTemp = System.getProperty("java.io.tmpdir");
    if (StringUtils.hasText(systemTemp)) {
      return Paths.get(systemTemp).resolve("famora-backup-upload-sessions")
          .toAbsolutePath().normalize();
    }
    return Paths.get("famora-backup-upload-sessions").toAbsolutePath().normalize();
  }
}
