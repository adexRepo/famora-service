package com.famora.backup.dto;

import com.famora.backup.entity.BackupUploadItem;
import com.famora.backup.entity.BackupUploadSession;
import com.famora.backup.enums.BackupUploadItemStatus;
import com.famora.backup.enums.BackupUploadSessionStatus;
import com.famora.common.helper.Visibility;
import com.famora.file.dto.FileDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BackupUploadDtos {
  
  public record CreateBackupSessionRequest(
      @NotEmpty List<@Valid BackupUploadItemRequest> files,
      String category,
      String notes,
      Visibility visibility,
      Map<String, Object> metadataJson
  ) {
  
  }
  
  public record BackupUploadItemRequest(
      String clientFileId,
      @NotBlank String originalName,
      String originalMimeType,
      @Positive long fileSize,
      String sha256,
      @Positive long chunkSize,
      @Min(1) int totalChunks,
      String category,
      String notes,
      Visibility visibility,
      Map<String, Object> metadataJson
  ) {
  
  }
  
  public record BackupSessionResponse(
      UUID id,
      BackupUploadSessionStatus uploadStatus,
      int totalFiles,
      int completedFiles,
      int failedFiles,
      long totalBytes,
      long uploadedBytes,
      double progressPercentage,
      String category,
      String notes,
      Visibility visibility,
      Map<String, Object> metadataJson,
      OffsetDateTime completedAt,
      OffsetDateTime cancelledAt,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {
    
    public static BackupSessionResponse from(BackupUploadSession session) {
      return new BackupSessionResponse(
          session.getId(),
          session.getUploadStatus(),
          session.getTotalFiles(),
          session.getCompletedFiles(),
          session.getFailedFiles(),
          session.getTotalBytes(),
          session.getUploadedBytes(),
          percentage(session.getUploadedBytes(), session.getTotalBytes()),
          session.getCategory(),
          session.getNotes(),
          session.getVisibility(),
          session.getMetadataJson(),
          session.getCompletedAt(),
          session.getCancelledAt(),
          session.getCreatedAt(),
          session.getUpdatedAt()
      );
    }
  }
  
  public record BackupSessionDetailResponse(
      UUID id,
      BackupUploadSessionStatus uploadStatus,
      int totalFiles,
      int completedFiles,
      int failedFiles,
      long totalBytes,
      long uploadedBytes,
      double progressPercentage,
      String category,
      String notes,
      Visibility visibility,
      Map<String, Object> metadataJson,
      List<BackupItemResponse> items,
      OffsetDateTime completedAt,
      OffsetDateTime cancelledAt,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {
    
    public static BackupSessionDetailResponse from(BackupUploadSession session,
        List<BackupUploadItem> items) {
      return new BackupSessionDetailResponse(
          session.getId(),
          session.getUploadStatus(),
          session.getTotalFiles(),
          session.getCompletedFiles(),
          session.getFailedFiles(),
          session.getTotalBytes(),
          session.getUploadedBytes(),
          percentage(session.getUploadedBytes(), session.getTotalBytes()),
          session.getCategory(),
          session.getNotes(),
          session.getVisibility(),
          session.getMetadataJson(),
          items.stream().map(BackupItemResponse::from).toList(),
          session.getCompletedAt(),
          session.getCancelledAt(),
          session.getCreatedAt(),
          session.getUpdatedAt()
      );
    }
  }
  
  public record BackupItemResponse(
      UUID id,
      UUID sessionId,
      UUID fileId,
      String clientFileId,
      String originalName,
      String originalMimeType,
      long fileSize,
      String expectedSha256,
      String assembledSha256,
      long chunkSize,
      int totalChunks,
      int receivedChunks,
      long uploadedBytes,
      double progressPercentage,
      BackupUploadItemStatus itemStatus,
      String category,
      String notes,
      Visibility visibility,
      Map<String, Object> metadataJson,
      FileDtos.FileResponse file,
      OffsetDateTime completedAt,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {
    
    public static BackupItemResponse from(BackupUploadItem item) {
      return new BackupItemResponse(
          item.getId(),
          item.getSession().getId(),
          item.getFileAsset() == null ? null : item.getFileAsset().getId(),
          item.getClientFileId(),
          item.getOriginalName(),
          item.getOriginalMimeType(),
          item.getFileSize(),
          item.getExpectedSha256(),
          item.getAssembledSha256(),
          item.getChunkSize(),
          item.getTotalChunks(),
          item.getReceivedChunks(),
          item.getUploadedBytes(),
          percentage(item.getUploadedBytes(), item.getFileSize()),
          item.getItemStatus(),
          item.getCategory(),
          item.getNotes(),
          item.getVisibility(),
          item.getMetadataJson(),
          item.getFileAsset() == null ? null : FileDtos.FileResponse.from(item.getFileAsset()),
          item.getCompletedAt(),
          item.getCreatedAt(),
          item.getUpdatedAt()
      );
    }
  }
  
  private static double percentage(long uploadedBytes, long totalBytes) {
    if (totalBytes <= 0) {
      return 0;
    }
    double value = (uploadedBytes * 100.0) / totalBytes;
    return Math.round(value * 100.0) / 100.0;
  }
}
