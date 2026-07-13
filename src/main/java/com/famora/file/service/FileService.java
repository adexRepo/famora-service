package com.famora.file.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.helper.PermissionHelper;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.common.spec.VisibleFamilyScopedSpecifications;
import com.famora.family.dto.FamilyContext;
import com.famora.file.dto.FileDtos;
import com.famora.file.dto.StoredFile;
import com.famora.file.entity.FileAsset;
import com.famora.file.helper.FileType;
import com.famora.file.helper.StorageType;
import com.famora.file.repository.FileRepository;
import com.famora.file.spec.FileAssetSpecifications;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileService {
  
  private final FileRepository repo;
  private final StorageService storage;
  private final AuditLogService audit;
  
  @Value("${app.storage.type:MINIO}")
  private StorageType defaultStorageType;
  @Value("${app.storage.document-max-upload-bytes:52428800}")
  private long documentMaxUploadBytes;
  
  public FileAsset upload(
      MultipartFile file,
      String category,
      String notes,
      Visibility visibility,
      FamilyContext ctx,
      String bucket
  ) {
    if ("DOCUMENT".equalsIgnoreCase(category) || "documents".equalsIgnoreCase(bucket)) {
      storage.validateMaxUploadSize(file, documentMaxUploadBytes, "Document file");
    }
    
    StoredFile stored = storage.store(
        defaultStorageType,
        file,
        ctx.family().getId(),
        bucket
    );
    
    FileAsset fa = new FileAsset();
    
    fa.setFamily(ctx.family());
    fa.setCreatedBy(ctx.user());
    
    fa.setOriginalName(stored.originalName());
    fa.setOriginalExtension(stored.originalExtension());
    fa.setOriginalMimeType(stored.originalMimeType());
    
    fa.setStoredName(stored.storedName());
    
    // Important: save storage provider
    fa.setStorageType(defaultStorageType);
    
    // Local/MFT field
    fa.setStoragePath(stored.storagePath());
    
    // MinIO fields
    fa.setBucketName(stored.bucketName());
    fa.setObjectKey(stored.objectKey());
    
    fa.setMimeType(stored.mimeType());
    fa.setFileType(stored.fileType());
    fa.setFileSize(file.getSize());
    fa.setFileHash(stored.sha256());
    fa.setMetadataJson(stored.metadataJson());
    
    fa.setCategory(category);
    fa.setNotes(notes);
    fa.setVisibility(visibility == null ? Visibility.PRIVATE : visibility);
    
    repo.save(fa);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.FILE_CREATED,
        "files",
        fa.getId(),
        "{\"fileId\":\"" + fa.getId()
            + "\",\"originalName\":\"" + fa.getOriginalName()
            + "\",\"fileSize\":" + fa.getFileSize()
            + ",\"storageType\":\"" + fa.getStorageType()
            + "\"}"
    );
    
    return fa;
  }
  
  @Transactional(readOnly = true)
  public Page<FileAsset> list(
      FamilyContext ctx,
      String keyword,
      FileType fileType,
      Visibility visibility,
      Pageable pageable
  ) {
    UUID familyId = ctx.family().getId();
    UUID userId = ctx.user().getId();
    boolean isOwner = ctx.owner();
    
    Specification<FileAsset> spec = Specification
        .where(VisibleFamilyScopedSpecifications.<FileAsset>visibleToUser(
            familyId,
            userId,
            isOwner,
            Status.ACTIVE,
            visibility
        ))
        .and(FileAssetSpecifications.keyword(keyword))
        .and(FileAssetSpecifications.fileType(fileType));
    
    return repo.findAll(spec, pageable);
  }
  
  public FileAsset get(UUID id, FamilyContext ctx) {
    FileAsset f = repo.findByIdAndFamilyIdAndStatus(
            id,
            ctx.family().getId(),
            Status.ACTIVE
        )
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "File not found"));
    
    PermissionHelper.assertCanAccess(f.getVisibility(), f.getStatus(), f.getCreatedBy().getId(),
        ctx);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.FILE_VIEWED,
        "files",
        f.getId(),
        "{\"fileId\":\"" + id + "\"}"
    );
    
    return f;
  }
  
  public Download download(UUID id, FamilyContext ctx) {
    FileAsset f = get(id, ctx);
    
    Resource resource = loadResource(f);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.FILE_DOWNLOADED,
        "files",
        f.getId(),
        "{\"fileId\":\"" + id
            + "\",\"storageType\":\"" + f.getStorageType()
            + "\"}"
    );
    
    return new Download(f, resource);
  }
  
  private Resource loadResource(FileAsset f) {
    StorageType storageType = f.getStorageType();
    
    if (storageType == null) {
      throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "File storage type is missing");
    }
    
    if (storageType == StorageType.MINIO) {
      if (!StringUtils.hasText(f.getObjectKey())) {
        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO object key is missing");
      }
      
      return storage.load(
          StorageType.MINIO,
          f.getBucketName(),
          f.getObjectKey()
      );
    }
    
    if (storageType == StorageType.MFT) {
      if (!StringUtils.hasText(f.getStoragePath())) {
        throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Storage path is missing");
      }
      
      return storage.load(
          StorageType.MINIO,
          f.getBucketName(),
          f.getObjectKey()
      );
    }
    
    throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Unsupported storage type");
  }
  
  public FileAsset update(UUID id, FileDtos.UpdateFileRequest req, FamilyContext ctx) {
    FileAsset f = get(id, ctx);
    
    f.setCategory(req.category());
    f.setNotes(req.notes());
    
    if (req.visibility() != null) {
      f.setVisibility(req.visibility());
    }
    
    repo.save(f);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.FILE_UPDATED,
        "files",
        f.getId(),
        "{\"fileId\":\"" + id + "\"}"
    );
    
    return f;
  }
  
  public void delete(UUID id, FamilyContext ctx) {
    FileAsset f = get(id, ctx);
    
    f.setStatus(Status.DELETED);
    
    repo.save(f);
    
    audit.log(
        ctx.family(),
        ctx.user(),
        AuditAction.FILE_DELETED,
        "files",
        f.getId(),
        "{\"fileId\":\"" + id
            + "\",\"storageType\":\"" + f.getStorageType()
            + "\"}"
    );
  }
  
  public record Download(FileAsset file, Resource resource) {
  
  }
}
