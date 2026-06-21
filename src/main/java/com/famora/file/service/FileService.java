package com.famora.file.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.helper.PermissionHelper;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.family.dto.FamilyContext;
import com.famora.file.dto.FileDtos;
import com.famora.file.dto.StoredFile;
import com.famora.file.entity.FileAsset;
import com.famora.file.helper.FileType;
import com.famora.file.helper.StorageType;
import com.famora.file.repository.FileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
  
  public FileAsset upload(
      MultipartFile file,
      String category,
      String notes,
      Visibility visibility,
      FamilyContext ctx,
      String bucket
  ) {
    
    StoredFile stored = storage.store(
        defaultStorageType,
        file,
        ctx.family().getId(),
        bucket
    );
    
    FileAsset fa = new FileAsset();
    
    fa.setFamily(ctx.family());
    fa.setCreatedBy(ctx.user());
    
    fa.setOriginalName(StringUtils.cleanPath(
        file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()
    ));
    
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
  
  public Page<FileAsset> list(
      FamilyContext ctx,
      String keyword,
      FileType fileType,
      Visibility visibility,
      Pageable pageable
  ) {
    Page<FileAsset> page;
    
    if (fileType != null && visibility != null) {
      page = repo.findAllByFamilyIdAndStatusAndFileTypeAndVisibility(
          ctx.family().getId(),
          Status.ACTIVE,
          fileType,
          visibility,
          pageable
      );
    } else if (fileType != null) {
      page = repo.findAllByFamilyIdAndStatusAndFileType(
          ctx.family().getId(),
          Status.ACTIVE,
          fileType,
          pageable
      );
    } else if (visibility != null) {
      page = repo.findAllByFamilyIdAndStatusAndVisibility(
          ctx.family().getId(),
          Status.ACTIVE,
          visibility,
          pageable
      );
    } else {
      page = repo.findAllByFamilyIdAndStatus(
          ctx.family().getId(),
          Status.ACTIVE,
          pageable
      );
    }
    
    var visible = page.getContent().stream().filter(f -> {
      try {
        PermissionHelper.assertCanAccess(f.getVisibility(), f.getStatus(), f.getCreatedBy().getId(),
            ctx);
        return true;
      } catch (AppException ex) {
        return false;
      }
    }).toList();
    
    return new PageImpl<>(visible, pageable, visible.size());
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
