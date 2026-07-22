package com.famora.backup.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.backup.dto.BackupUploadDtos.BackupItemResponse;
import com.famora.backup.dto.BackupUploadDtos.BackupSessionDetailResponse;
import com.famora.backup.dto.BackupUploadDtos.BackupSessionResponse;
import com.famora.backup.dto.BackupUploadDtos.BackupUploadItemRequest;
import com.famora.backup.dto.BackupUploadDtos.CreateBackupSessionRequest;
import com.famora.backup.entity.BackupUploadChunk;
import com.famora.backup.entity.BackupUploadItem;
import com.famora.backup.entity.BackupUploadSession;
import com.famora.backup.enums.BackupUploadItemStatus;
import com.famora.backup.enums.BackupUploadSessionStatus;
import com.famora.backup.repository.BackupUploadChunkRepository;
import com.famora.backup.repository.BackupUploadItemRepository;
import com.famora.backup.repository.BackupUploadSessionRepository;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.family.dto.FamilyContext;
import com.famora.file.entity.FileAsset;
import com.famora.file.service.FileService;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BackupUploadService {
  
  private static final String DEFAULT_CATEGORY = "BACKUP";
  
  private final BackupUploadSessionRepository sessionRepository;
  private final BackupUploadItemRepository itemRepository;
  private final BackupUploadChunkRepository chunkRepository;
  private final FileService fileService;
  private final AuditLogService audit;
  
  @Value("${app.backup.temp-root:/app/storage/backup-upload-sessions}")
  private String tempRoot;
  
  @Value("${app.backup.max-chunk-bytes:5242880}")
  private long maxChunkBytes;
  
  @Value("${app.backup.max-file-bytes:104857600}")
  private long maxFileBytes;
  
  @Transactional
  public BackupSessionDetailResponse createSession(CreateBackupSessionRequest request,
      FamilyContext ctx) {
    validateCreateRequest(request);
    
    BackupUploadSession session = new BackupUploadSession();
    session.setFamily(ctx.family());
    session.setCreatedBy(ctx.user());
    session.setUploadStatus(BackupUploadSessionStatus.PENDING);
    session.setTotalFiles(request.files().size());
    session.setTotalBytes(request.files().stream().mapToLong(BackupUploadItemRequest::fileSize)
        .sum());
    session.setUploadedBytes(0);
    session.setCompletedFiles(0);
    session.setFailedFiles(0);
    session.setCategory(defaultString(request.category(), DEFAULT_CATEGORY));
    session.setNotes(request.notes());
    session.setVisibility(defaultVisibility(request.visibility()));
    session.setMetadataJson(request.metadataJson());
    sessionRepository.save(session);
    
    List<BackupUploadItem> items = request.files().stream()
        .map(itemRequest -> createItem(session, itemRequest, ctx))
        .toList();
    itemRepository.saveAll(items);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.BACKUP_SESSION_CREATED,
        "backup_upload_sessions",
        session.getId(),
        "{\"sessionId\":\"" + session.getId()
            + "\",\"totalFiles\":" + session.getTotalFiles()
            + ",\"totalBytes\":" + session.getTotalBytes() + "}"
    );
    
    return BackupSessionDetailResponse.from(session, items);
  }
  
  @Transactional(readOnly = true)
  public Page<BackupSessionResponse> listSessions(FamilyContext ctx, Pageable pageable) {
    return sessionRepository.findByFamilyIdAndStatus(ctx.family().getId(), Status.ACTIVE,
        pageable).map(BackupSessionResponse::from);
  }
  
  @Transactional(readOnly = true)
  public BackupSessionDetailResponse getSession(UUID sessionId, FamilyContext ctx) {
    BackupUploadSession session = requireSession(sessionId, ctx);
    return BackupSessionDetailResponse.from(session, activeItems(session));
  }
  
  @Transactional
  public BackupItemResponse uploadChunk(UUID sessionId, UUID itemId, int chunkNumber,
      MultipartFile chunkFile, String sha256, FamilyContext ctx) {
    BackupUploadSession session = requireMutableSession(sessionId, ctx);
    BackupUploadItem item = requireMutableItem(session, itemId);
    
    validateChunkRequest(item, chunkNumber, chunkFile, sha256);
    
    StoredChunk storedChunk = storeChunk(session, item, chunkNumber, chunkFile);
    String normalizedSha256 = normalizedSha256(sha256);
    if (StringUtils.hasText(normalizedSha256) && !normalizedSha256.equals(storedChunk.sha256())) {
      removePathIfExists(storedChunk.path());
      throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "Chunk checksum mismatch");
    }
    
    BackupUploadChunk chunk = chunkRepository
        .findByItemIdAndChunkNumberAndStatus(item.getId(), chunkNumber, Status.ACTIVE)
        .orElseGet(BackupUploadChunk::new);
    if (chunk.getId() == null) {
      chunk.setSession(session);
      chunk.setItem(item);
      chunk.setChunkNumber(chunkNumber);
      chunk.setCreatedBy(ctx.user());
    } else if (StringUtils.hasText(chunk.getStoragePath())) {
      removePathIfExists(Paths.get(chunk.getStoragePath()));
      chunk.setUpdatedBy(ctx.user());
    }
    chunk.setChunkSize(storedChunk.size());
    chunk.setSha256(storedChunk.sha256());
    chunk.setStoragePath(storedChunk.path().toString());
    chunkRepository.save(chunk);
    
    refreshItemProgress(item, ctx);
    session.setUploadStatus(BackupUploadSessionStatus.IN_PROGRESS);
    session.setUpdatedBy(ctx.user());
    refreshSessionProgress(session);
    
    return BackupItemResponse.from(item);
  }
  
  @Transactional
  public BackupItemResponse completeItem(UUID sessionId, UUID itemId, FamilyContext ctx) {
    BackupUploadSession session = requireMutableSession(sessionId, ctx);
    BackupUploadItem item = requireItem(session, itemId);
    
    if (item.getItemStatus() == BackupUploadItemStatus.COMPLETED) {
      return BackupItemResponse.from(item);
    }
    if (item.getItemStatus() == BackupUploadItemStatus.CANCELLED) {
      throw new AppException(HttpStatus.CONFLICT, "Backup item is cancelled");
    }
    
    List<BackupUploadChunk> chunks = chunkRepository.findByItemIdAndStatusOrderByChunkNumberAsc(
        item.getId(), Status.ACTIVE);
    if (chunks.size() != item.getTotalChunks()) {
      throw new AppException(HttpStatus.CONFLICT, "Backup item still has missing chunks");
    }
    
    Path assembled = assembleItem(item, chunks);
    String assembledHash = sha256(assembled);
    String expectedHash = normalizedSha256(item.getExpectedSha256());
    if (StringUtils.hasText(expectedHash) && !expectedHash.equals(assembledHash)) {
      removePathIfExists(assembled);
      throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "File checksum mismatch");
    }
    
    MultipartFile multipartFile = new PathMultipartFile(
        "file",
        item.getOriginalName(),
        item.getOriginalMimeType(),
        assembled
    );
    FileAsset file = fileService.upload(
        multipartFile,
        defaultString(item.getCategory(), DEFAULT_CATEGORY),
        item.getNotes(),
        item.getVisibility(),
        ctx,
        "backups"
    );
    
    item.setFileAsset(file);
    item.setAssembledSha256(assembledHash);
    item.setUploadedBytes(item.getFileSize());
    item.setReceivedChunks(item.getTotalChunks());
    item.setItemStatus(BackupUploadItemStatus.COMPLETED);
    item.setCompletedAt(now());
    item.setUpdatedBy(ctx.user());
    itemRepository.save(item);
    
    refreshSessionProgress(session);
    session.setUpdatedBy(ctx.user());
    
    cleanupItemTemp(item);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.BACKUP_ITEM_COMPLETED,
        "backup_upload_items",
        item.getId(),
        "{\"sessionId\":\"" + session.getId()
            + "\",\"itemId\":\"" + item.getId()
            + "\",\"fileId\":\"" + file.getId() + "\"}"
    );
    
    if (session.getUploadStatus() == BackupUploadSessionStatus.COMPLETED) {
      audit.log(
          ctx.family(),
          ctx.user(),
          AuditAction.BACKUP_SESSION_COMPLETED,
          "backup_upload_sessions",
          session.getId(),
          "{\"sessionId\":\"" + session.getId() + "\"}"
      );
    }
    
    return BackupItemResponse.from(item);
  }
  
  @Transactional
  public BackupSessionDetailResponse cancelSession(UUID sessionId, FamilyContext ctx) {
    BackupUploadSession session = requireSession(sessionId, ctx);
    if (session.getUploadStatus() == BackupUploadSessionStatus.COMPLETED) {
      throw new AppException(HttpStatus.CONFLICT, "Completed backup session cannot be cancelled");
    }
    session.setUploadStatus(BackupUploadSessionStatus.CANCELLED);
    session.setCancelledAt(now());
    session.setUpdatedBy(ctx.user());
    
    List<BackupUploadItem> items = activeItems(session);
    for (BackupUploadItem item : items) {
      if (item.getItemStatus() != BackupUploadItemStatus.COMPLETED) {
        item.setItemStatus(BackupUploadItemStatus.CANCELLED);
        item.setUpdatedBy(ctx.user());
      }
      cleanupItemTemp(item);
    }
    itemRepository.saveAll(items);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.BACKUP_SESSION_CANCELLED,
        "backup_upload_sessions",
        session.getId(),
        "{\"sessionId\":\"" + session.getId() + "\"}"
    );
    
    return BackupSessionDetailResponse.from(session, items);
  }
  
  private BackupUploadItem createItem(BackupUploadSession session, BackupUploadItemRequest request,
      FamilyContext ctx) {
    BackupUploadItem item = new BackupUploadItem();
    item.setSession(session);
    item.setFamily(ctx.family());
    item.setCreatedBy(ctx.user());
    item.setClientFileId(cleanOptional(request.clientFileId()));
    item.setOriginalName(cleanFilename(request.originalName()));
    item.setOriginalMimeType(cleanOptional(request.originalMimeType()));
    item.setFileSize(request.fileSize());
    item.setExpectedSha256(normalizedSha256(request.sha256()));
    item.setChunkSize(request.chunkSize());
    item.setTotalChunks(request.totalChunks());
    item.setReceivedChunks(0);
    item.setUploadedBytes(0);
    item.setItemStatus(BackupUploadItemStatus.PENDING);
    item.setCategory(defaultString(request.category(), session.getCategory()));
    item.setNotes(request.notes() == null ? session.getNotes() : request.notes());
    item.setVisibility(request.visibility() == null ? session.getVisibility() : request.visibility());
    item.setMetadataJson(request.metadataJson());
    return item;
  }
  
  private void validateCreateRequest(CreateBackupSessionRequest request) {
    if (request == null || request.files() == null || request.files().isEmpty()) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Backup files are required");
    }
    for (BackupUploadItemRequest file : request.files()) {
      if (file.fileSize() <= 0) {
        throw new AppException(HttpStatus.BAD_REQUEST, "File size must be greater than zero");
      }
      if (file.fileSize() > maxFileBytes) {
        throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE,
            "Backup file exceeds max size of " + maxFileBytes + " bytes");
      }
      if (file.chunkSize() <= 0 || file.chunkSize() > maxChunkBytes) {
        throw new AppException(HttpStatus.BAD_REQUEST,
            "Chunk size must be between 1 and " + maxChunkBytes + " bytes");
      }
      long expectedTotalChunks = (file.fileSize() + file.chunkSize() - 1) / file.chunkSize();
      if (file.totalChunks() != expectedTotalChunks) {
        throw new AppException(HttpStatus.BAD_REQUEST,
            "Invalid totalChunks for file " + file.originalName());
      }
      normalizedSha256(file.sha256());
      cleanFilename(file.originalName());
    }
  }
  
  private void validateChunkRequest(BackupUploadItem item, int chunkNumber, MultipartFile chunkFile,
      String sha256) {
    if (chunkNumber < 1 || chunkNumber > item.getTotalChunks()) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Invalid chunk number");
    }
    if (chunkFile == null || chunkFile.isEmpty()) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Chunk file is required");
    }
    if (chunkFile.getSize() > maxChunkBytes) {
      throw new AppException(HttpStatus.PAYLOAD_TOO_LARGE,
          "Chunk exceeds max size of " + maxChunkBytes + " bytes");
    }
    long expectedSize = expectedChunkSize(item, chunkNumber);
    if (chunkFile.getSize() != expectedSize) {
      throw new AppException(HttpStatus.BAD_REQUEST,
          "Invalid chunk size. Expected " + expectedSize + " bytes");
    }
    normalizedSha256(sha256);
  }
  
  private long expectedChunkSize(BackupUploadItem item, int chunkNumber) {
    if (chunkNumber < item.getTotalChunks()) {
      return item.getChunkSize();
    }
    long consumedBytes = item.getChunkSize() * (item.getTotalChunks() - 1L);
    return item.getFileSize() - consumedBytes;
  }
  
  private StoredChunk storeChunk(BackupUploadSession session, BackupUploadItem item,
      int chunkNumber, MultipartFile file) {
    Path dir = itemTempDir(session, item);
    try {
      Files.createDirectories(dir);
      Path target = dir.resolve("%06d.part".formatted(chunkNumber)).normalize();
      ensureInside(dir, target);
      
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream in = file.getInputStream();
          OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE,
              StandardOpenOption.TRUNCATE_EXISTING)) {
        byte[] buffer = new byte[8192];
        int length;
        while ((length = in.read(buffer)) != -1) {
          digest.update(buffer, 0, length);
          out.write(buffer, 0, length);
        }
      }
      return new StoredChunk(target, file.getSize(), HexFormat.of().formatHex(digest.digest()));
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to store backup chunk: " + e.getMessage());
    }
  }
  
  private Path assembleItem(BackupUploadItem item, List<BackupUploadChunk> chunks) {
    Path dir = itemTempDir(item.getSession(), item);
    Path assembled = dir.resolve("assembled-" + item.getId()).normalize();
    ensureInside(dir, assembled);
    
    try (OutputStream out = Files.newOutputStream(assembled, StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING)) {
      for (int i = 0; i < chunks.size(); i++) {
        BackupUploadChunk chunk = chunks.get(i);
        int expectedNumber = i + 1;
        if (chunk.getChunkNumber() != expectedNumber) {
          throw new AppException(HttpStatus.CONFLICT,
              "Backup item has missing chunk number " + expectedNumber);
        }
        Path chunkPath = Paths.get(chunk.getStoragePath()).toAbsolutePath().normalize();
        ensureInside(dir, chunkPath);
        Files.copy(chunkPath, out);
      }
      return assembled;
    } catch (AppException e) {
      throw e;
    } catch (IOException e) {
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to assemble backup file: " + e.getMessage());
    }
  }
  
  private String sha256(Path path) {
    try (InputStream in = Files.newInputStream(path)) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] buffer = new byte[8192];
      int length;
      while ((length = in.read(buffer)) != -1) {
        digest.update(buffer, 0, length);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (Exception e) {
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to calculate checksum: " + e.getMessage());
    }
  }
  
  private void refreshItemProgress(BackupUploadItem item, FamilyContext ctx) {
    int receivedChunks = Math.toIntExact(chunkRepository.countByItemIdAndStatus(item.getId(),
        Status.ACTIVE));
    long uploadedBytes = chunkRepository.sumChunkSize(item.getId(), Status.ACTIVE);
    
    item.setReceivedChunks(receivedChunks);
    item.setUploadedBytes(Math.min(uploadedBytes, item.getFileSize()));
    item.setItemStatus(receivedChunks == item.getTotalChunks()
        ? BackupUploadItemStatus.READY_TO_COMPLETE
        : BackupUploadItemStatus.UPLOADING);
    item.setUpdatedBy(ctx.user());
    itemRepository.save(item);
  }
  
  private void refreshSessionProgress(BackupUploadSession session) {
    session.setCompletedFiles(Math.toIntExact(itemRepository.countBySessionIdAndStatusAndItemStatus(
        session.getId(), Status.ACTIVE, BackupUploadItemStatus.COMPLETED)));
    session.setFailedFiles(Math.toIntExact(itemRepository.countBySessionIdAndStatusAndItemStatus(
        session.getId(), Status.ACTIVE, BackupUploadItemStatus.FAILED)));
    session.setUploadedBytes(Math.min(itemRepository.sumUploadedBytes(session.getId(),
        Status.ACTIVE), session.getTotalBytes()));
    if (session.getCompletedFiles() == session.getTotalFiles()) {
      session.setUploadStatus(BackupUploadSessionStatus.COMPLETED);
      if (session.getCompletedAt() == null) {
        session.setCompletedAt(now());
      }
    } else if (session.getUploadStatus() != BackupUploadSessionStatus.CANCELLED) {
      session.setUploadStatus(BackupUploadSessionStatus.IN_PROGRESS);
    }
    sessionRepository.save(session);
  }
  
  private BackupUploadSession requireSession(UUID sessionId, FamilyContext ctx) {
    return sessionRepository.findByIdAndFamilyIdAndStatus(sessionId, ctx.family().getId(),
            Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Backup session not found"));
  }
  
  private BackupUploadSession requireMutableSession(UUID sessionId, FamilyContext ctx) {
    BackupUploadSession session = requireSession(sessionId, ctx);
    if (session.getUploadStatus() == BackupUploadSessionStatus.COMPLETED
        || session.getUploadStatus() == BackupUploadSessionStatus.CANCELLED) {
      throw new AppException(HttpStatus.CONFLICT, "Backup session is not mutable");
    }
    return session;
  }
  
  private BackupUploadItem requireItem(BackupUploadSession session, UUID itemId) {
    return itemRepository.findByIdAndSessionIdAndStatus(itemId, session.getId(), Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Backup item not found"));
  }
  
  private BackupUploadItem requireMutableItem(BackupUploadSession session, UUID itemId) {
    BackupUploadItem item = requireItem(session, itemId);
    if (item.getItemStatus() == BackupUploadItemStatus.COMPLETED
        || item.getItemStatus() == BackupUploadItemStatus.CANCELLED) {
      throw new AppException(HttpStatus.CONFLICT, "Backup item is not mutable");
    }
    return item;
  }
  
  private List<BackupUploadItem> activeItems(BackupUploadSession session) {
    return itemRepository.findBySessionIdAndStatusOrderByCreatedAtAsc(session.getId(),
        Status.ACTIVE);
  }
  
  private Path itemTempDir(BackupUploadSession session, BackupUploadItem item) {
    Path root = Paths.get(tempRoot).toAbsolutePath().normalize();
    Path dir = root
        .resolve(session.getFamily().getId().toString())
        .resolve(session.getId().toString())
        .resolve(item.getId().toString())
        .normalize();
    ensureInside(root, dir);
    return dir;
  }
  
  private void cleanupItemTemp(BackupUploadItem item) {
    Path dir = itemTempDir(item.getSession(), item);
    if (!Files.exists(dir)) {
      return;
    }
    try (var paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    } catch (IOException ignored) {
      // Cleanup failure should not break a completed backup item.
    }
  }
  
  private void removePathIfExists(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path.toAbsolutePath().normalize());
    } catch (IOException ignored) {
      // A stale temp chunk can be overwritten by the next retry.
    }
  }
  
  private void ensureInside(Path base, Path target) {
    if (!target.toAbsolutePath().normalize().startsWith(base.toAbsolutePath().normalize())) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Invalid backup storage path");
    }
  }
  
  private String cleanFilename(String name) {
    String clean = StringUtils.cleanPath(name == null ? "" : name.trim());
    if (!StringUtils.hasText(clean) || clean.contains("..") || clean.contains("/")
        || clean.contains("\\")) {
      throw new AppException(HttpStatus.BAD_REQUEST, "Invalid filename");
    }
    return clean;
  }
  
  private String cleanOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
  
  private String normalizedSha256(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    if (!normalized.matches("[a-f0-9]{64}")) {
      throw new AppException(HttpStatus.BAD_REQUEST, "SHA-256 checksum must be 64 hex chars");
    }
    return normalized;
  }
  
  private String defaultString(String value, String fallback) {
    return StringUtils.hasText(value) ? value.trim() : fallback;
  }
  
  private Visibility defaultVisibility(Visibility visibility) {
    return visibility == null ? Visibility.PRIVATE : visibility;
  }
  
  private OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
  
  private record StoredChunk(Path path, long size, String sha256) {
  
  }
  
  private record PathMultipartFile(String name, String originalFilename, String contentType,
                                   Path path) implements MultipartFile {
    
    @Override
    public String getName() {
      return name;
    }
    
    @Override
    public String getOriginalFilename() {
      return originalFilename;
    }
    
    @Override
    public String getContentType() {
      return contentType;
    }
    
    @Override
    public boolean isEmpty() {
      return getSize() == 0;
    }
    
    @Override
    public long getSize() {
      try {
        return Files.size(path);
      } catch (IOException e) {
        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read assembled file");
      }
    }
    
    @Override
    public byte[] getBytes() throws IOException {
      return Files.readAllBytes(path);
    }
    
    @Override
    public InputStream getInputStream() throws IOException {
      return Files.newInputStream(path);
    }
    
    @Override
    public Resource getResource() {
      return MultipartFile.super.getResource();
    }
    
    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
      Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
    
    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
      Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
    }
  }
}
