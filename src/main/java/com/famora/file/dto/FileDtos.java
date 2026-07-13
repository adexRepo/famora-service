package com.famora.file.dto;

import com.famora.common.helper.Visibility;
import com.famora.file.entity.FileAsset;
import com.famora.file.helper.FileType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class FileDtos {
  
  public record FileResponse(UUID id, String originalName, String mimeType, FileType fileType,
                             String originalExtension, String originalMimeType,
                             long fileSize, String fileHash, Map<String, Object> metadataJson,
                             String category, String notes,
                             Visibility visibility, OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {
    
    public static FileResponse from(FileAsset f) {
      return new FileResponse(f.getId(), f.getOriginalName(), f.getMimeType(), f.getFileType(),
          f.getOriginalExtension(), f.getOriginalMimeType(), f.getFileSize(), f.getFileHash(),
          f.getMetadataJson(), f.getCategory(), f.getNotes(), f.getVisibility(), f.getCreatedAt(),
          f.getUpdatedAt());
    }
  }
  
  public record UpdateFileRequest(String category, String notes, Visibility visibility) {
  
  }
}
