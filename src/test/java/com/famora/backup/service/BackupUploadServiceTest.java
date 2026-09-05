package com.famora.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.audit.service.AuditLogService;
import com.famora.backup.dto.BackupUploadDtos.BackupUploadItemRequest;
import com.famora.backup.dto.BackupUploadDtos.CreateBackupSessionRequest;
import com.famora.backup.enums.BackupUploadItemStatus;
import com.famora.backup.repository.BackupUploadChunkRepository;
import com.famora.backup.repository.BackupUploadItemRepository;
import com.famora.backup.repository.BackupUploadSessionRepository;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.file.service.FileService;
import com.famora.user.entity.User;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BackupUploadServiceTest {

  private static final long MEBIBYTE = 1024L * 1024L;

  @Mock private BackupUploadSessionRepository sessionRepository;
  @Mock private BackupUploadItemRepository itemRepository;
  @Mock private BackupUploadChunkRepository chunkRepository;
  @Mock private FileService fileService;
  @Mock private AuditLogService audit;
  @Mock private BackupStorageQuotaService quotaService;

  private BackupUploadService service;
  private FamilyContext context;

  @BeforeEach
  void setUp() {
    service = new BackupUploadService(sessionRepository, itemRepository, chunkRepository,
        fileService, audit, quotaService);
    ReflectionTestUtils.setField(service, "maxChunkBytes", 5 * MEBIBYTE);
    ReflectionTestUtils.setField(service, "maxFileBytes", 50 * MEBIBYTE);

    User user = User.builder().fullName("Uploader").email("uploader@example.com")
        .passwordHash("hash").build();
    Family family = Family.builder().name("Family").ownerUser(user).build();
    ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(family, "id", UUID.randomUUID());
    context = new FamilyContext(family, user, FamilyMemberRole.OWNER, true);
  }

  @Test
  void acceptsExactlyTwentyFilesAndFiftyMebibytesPerFile() {
    List<BackupUploadItemRequest> files = IntStream.range(0, 20)
        .mapToObj(index -> request("file-" + index + ".zip", 50 * MEBIBYTE))
        .toList();

    var response = service.createSession(new CreateBackupSessionRequest(
        files, null, null, null, null), context);

    assertThat(response.totalFiles()).isEqualTo(20);
    verify(quotaService).reserve(context.user().getId(), 20 * 50 * MEBIBYTE);
    verify(sessionRepository).save(any());
    verify(itemRepository).saveAll(any());
  }

  @Test
  void rejectsMoreThanTwentyFiles() {
    List<BackupUploadItemRequest> files = IntStream.range(0, 21)
        .mapToObj(index -> request("file-" + index + ".zip", 1))
        .toList();

    assertThatThrownBy(() -> service.createSession(new CreateBackupSessionRequest(
        files, null, null, null, null), context))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("at most 20 files");

    verify(quotaService, never()).reserve(any(), any(Long.class));
  }

  @Test
  void rejectsAFileLargerThanFiftyMebibytes() {
    assertThatThrownBy(() -> service.createSession(new CreateBackupSessionRequest(
        List.of(request("large.zip", 50 * MEBIBYTE + 1)), null, null, null, null), context))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("exceeds max size");

    verify(sessionRepository, never()).save(any());
  }

  @Test
  void rejectsCaseInsensitiveDuplicateNamesInOneSession() {
    var request = new CreateBackupSessionRequest(List.of(
        request("Tax.PDF", 1),
        request(" tax.pdf ", 1)), null, null, null, null);

    assertThatThrownBy(() -> service.createSession(request, context))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("Duplicate original filenames");

    verify(quotaService, never()).reserve(any(), any(Long.class));
  }

  @Test
  void rejectsANameReservedByAnotherActiveBackupFromTheSameUser() {
    UUID userId = context.user().getId();
    when(itemRepository
        .existsByCreatedBy_IdAndOriginalNameIgnoreCaseAndStatusAndItemStatusNot(
            userId, "archive.zip", Status.ACTIVE, BackupUploadItemStatus.CANCELLED))
        .thenReturn(true);

    AppException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
        () -> service.createSession(new CreateBackupSessionRequest(
            List.of(request("archive.zip", 1)), null, null, null, null), context),
        AppException.class);

    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(exception).hasMessageContaining("same original filename");
    verify(fileService, never()).validateOriginalNameAvailable(any(), any());
  }

  @Test
  void rejectsANameAlreadyStoredByTheSameUser() {
    UUID userId = context.user().getId();
    doThrow(new AppException(HttpStatus.CONFLICT,
        "A file with the same original filename already exists"))
        .when(fileService).validateOriginalNameAvailable("archive.zip", userId);

    assertThatThrownBy(() -> service.createSession(new CreateBackupSessionRequest(
        List.of(request("archive.zip", 1)), null, null, null, null), context))
        .isInstanceOf(AppException.class)
        .hasMessageContaining("same original filename");

    verify(sessionRepository, never()).save(any());
  }

  private BackupUploadItemRequest request(String originalName, long fileSize) {
    long chunkSize = Math.min(fileSize, 5 * MEBIBYTE);
    int totalChunks = Math.toIntExact((fileSize + chunkSize - 1) / chunkSize);
    return new BackupUploadItemRequest(null, originalName, "application/zip", fileSize, null,
        chunkSize, totalChunks, null, null, null, null);
  }
}
