package com.famora.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.backup.config.BackupQuotaProperties;
import com.famora.backup.entity.BackupUploadItem;
import com.famora.backup.enums.BackupUploadItemStatus;
import com.famora.backup.repository.BackupUploadItemRepository;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.file.entity.FileAsset;
import com.famora.user.entity.User;
import com.famora.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BackupStorageQuotaServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private BackupUploadItemRepository itemRepository;

  private BackupStorageQuotaService service;
  private User user;

  @BeforeEach
  void setUp() {
    service = new BackupStorageQuotaService(userRepository, itemRepository,
        new BackupQuotaProperties(100L));
    user = User.builder().fullName("Uploader").email("uploader@example.com")
        .passwordHash("hash").build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
  }

  @Test
  void reservesAvailableStorageAgainstLockedUser() {
    user.setBackupStorageUsedBytes(60);
    user.setBackupStorageReservedBytes(20);
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));

    service.reserve(user.getId(), 20);

    assertThat(user.getBackupStorageReservedBytes()).isEqualTo(40);
    verify(userRepository).save(user);
  }

  @Test
  void rejectsReservationBeyondEffectiveQuota() {
    user.setBackupStorageUsedBytes(60);
    user.setBackupStorageReservedBytes(20);
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));

    assertThatThrownBy(() -> service.reserve(user.getId(), 21))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("Available: 20 bytes");

    verify(userRepository, never()).save(user);
  }

  @Test
  void completionMovesReservedBytesToUsedBytes() {
    user.setBackupStorageUsedBytes(25);
    user.setBackupStorageReservedBytes(50);
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));

    service.completeReservation(user.getId(), 30);

    assertThat(user.getBackupStorageReservedBytes()).isEqualTo(20);
    assertThat(user.getBackupStorageUsedBytes()).isEqualTo(55);
  }

  @Test
  void deletingCompletedBackupReleasesUsedStorageFromOriginalUploader() {
    User deleter = User.builder().fullName("Deleter").email("deleter@example.com")
        .passwordHash("hash").build();
    user.setBackupStorageUsedBytes(80);
    BackupUploadItem item = new BackupUploadItem();
    item.setCreatedBy(user);
    item.setFileSize(30);
    FileAsset file = new FileAsset();
    ReflectionTestUtils.setField(file, "id", UUID.randomUUID());
    file.setCreatedBy(deleter);
    when(itemRepository.findFirstByFileAsset_IdAndItemStatusAndStatus(file.getId(),
        BackupUploadItemStatus.COMPLETED, Status.ACTIVE)).thenReturn(Optional.of(item));
    when(userRepository.findAllByIdForUpdate(List.of(user.getId()))).thenReturn(List.of(user));

    service.beforeDelete(file);

    assertThat(user.getBackupStorageUsedBytes()).isEqualTo(50);
  }

  @Test
  void usageUsesOverrideWithoutDeletingOverQuotaData() {
    user.setBackupQuotaOverrideEnabled(true);
    user.setBackupQuotaOverrideBytes(40L);
    user.setBackupStorageUsedBytes(50);

    var usage = service.usage(user);

    assertThat(usage.effectiveQuotaBytes()).isEqualTo(40);
    assertThat(usage.availableBytes()).isZero();
    assertThat(usage.usedBytes()).isEqualTo(50);
  }
}
