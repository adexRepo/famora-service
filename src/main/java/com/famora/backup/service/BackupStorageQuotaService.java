package com.famora.backup.service;

import com.famora.backup.config.BackupQuotaProperties;
import com.famora.backup.enums.BackupUploadItemStatus;
import com.famora.backup.repository.BackupUploadItemRepository;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.file.entity.FileAsset;
import com.famora.file.service.FileDeletionListener;
import com.famora.user.entity.User;
import com.famora.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BackupStorageQuotaService implements FileDeletionListener {

  private final UserRepository userRepository;
  private final BackupUploadItemRepository itemRepository;
  private final BackupQuotaProperties properties;

  public void reserve(UUID userId, long bytes) {
    User user = lockUser(userId);
    long availableBytes = availableBytes(user);
    if (bytes > availableBytes) {
      throw new AppException(HttpStatus.CONFLICT,
          "Backup storage quota exceeded. Available: " + availableBytes + " bytes");
    }
    user.setBackupStorageReservedBytes(Math.addExact(user.getBackupStorageReservedBytes(), bytes));
    userRepository.save(user);
  }

  public void completeReservation(UUID userId, long bytes) {
    User user = lockUser(userId);
    user.setBackupStorageReservedBytes(
        Math.max(0, user.getBackupStorageReservedBytes() - bytes));
    user.setBackupStorageUsedBytes(Math.addExact(user.getBackupStorageUsedBytes(), bytes));
    userRepository.save(user);
  }

  public void releaseReservation(UUID userId, long bytes) {
    if (bytes <= 0) {
      return;
    }
    User user = lockUser(userId);
    user.setBackupStorageReservedBytes(
        Math.max(0, user.getBackupStorageReservedBytes() - bytes));
    userRepository.save(user);
  }

  @Override
  public void beforeDelete(FileAsset file) {
    var backupItem = itemRepository.findFirstByFileAsset_IdAndItemStatusAndStatus(file.getId(),
        BackupUploadItemStatus.COMPLETED, Status.ACTIVE);
    if (backupItem.isEmpty()) {
      return;
    }
    User user = lockUser(backupItem.get().getCreatedBy().getId());
    user.setBackupStorageUsedBytes(Math.max(0, user.getBackupStorageUsedBytes()
        - backupItem.get().getFileSize()));
    userRepository.save(user);
  }

  public BackupQuotaUsage usage(User user) {
    long defaultQuotaBytes = properties.effectiveDefaultBytes();
    long effectiveQuotaBytes = effectiveQuotaBytes(user);
    return new BackupQuotaUsage(
        defaultQuotaBytes,
        effectiveQuotaBytes,
        user.getBackupStorageUsedBytes(),
        user.getBackupStorageReservedBytes(),
        availableBytes(user),
        user.isBackupQuotaOverrideEnabled()
    );
  }

  private long effectiveQuotaBytes(User user) {
    return user.isBackupQuotaOverrideEnabled() && user.getBackupQuotaOverrideBytes() != null
        ? user.getBackupQuotaOverrideBytes()
        : properties.effectiveDefaultBytes();
  }

  private long availableBytes(User user) {
    long effectiveQuotaBytes = effectiveQuotaBytes(user);
    if (user.getBackupStorageUsedBytes() >= effectiveQuotaBytes) {
      return 0;
    }
    long remainingAfterUsage = effectiveQuotaBytes - user.getBackupStorageUsedBytes();
    return user.getBackupStorageReservedBytes() >= remainingAfterUsage
        ? 0
        : remainingAfterUsage - user.getBackupStorageReservedBytes();
  }

  private User lockUser(UUID userId) {
    return userRepository.findAllByIdForUpdate(List.of(userId)).stream()
        .findFirst()
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
  }

  public record BackupQuotaUsage(
      long defaultQuotaBytes,
      long effectiveQuotaBytes,
      long usedBytes,
      long reservedBytes,
      long availableBytes,
      boolean overrideEnabled
  ) {
  }
}
