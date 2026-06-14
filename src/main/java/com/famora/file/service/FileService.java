package com.famora.file.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.exception.Visibility;
import com.famora.common.helper.Status;
import com.famora.family.dto.FamilyContext;
import com.famora.file.dto.FileDtos;
import com.famora.file.entity.FileAsset;
import com.famora.file.helper.FileType;
import com.famora.file.repository.FileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
  private final FilePermissionService permission;
  private final AuditLogService audit;
  
  public FileAsset upload(MultipartFile file, String category, String notes, Visibility visibility,
      FamilyContext ctx, String bucket) {
    
    var stored = storage.store(file, ctx.familyId().getId(), bucket);
    FileAsset fa = new FileAsset();
    fa.setFamilyId(ctx.familyId().getId());
    fa.setUploadedByUserId(ctx.userId().getId());
    fa.setOriginalName(StringUtils.cleanPath(
        file.getOriginalFilename() == null ? "file" : file.getOriginalFilename()));
    fa.setStoredName(stored.storedName());
    fa.setStoragePath(stored.storagePath());
    fa.setMimeType(stored.mimeType());
    fa.setFileType(stored.fileType());
    fa.setFileSize(file.getSize());
    fa.setFileHash(stored.sha256());
    fa.setCategory(category);
    fa.setNotes(notes);
    fa.setVisibility(visibility == null ? Visibility.PRIVATE : visibility);
    repo.save(fa);
    
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.FILE_CREATED, "files", fa.getId(),
        "{\"fileId\":\"" + fa.getId() + "\",\"originalName\":\"" + fa.getOriginalName()
            + "\",\"fileSize\":" + fa.getFileSize() + "}");
    return fa;
  }
  
  public Page<FileAsset> list(FamilyContext ctx, String keyword, FileType fileType,
      Visibility visibility, Pageable pageable) {
    Page<FileAsset> page;
    if (fileType != null && visibility != null) {
      page = repo.findAllByFamilyIdAndStatusAndFileTypeAndVisibility(ctx.familyId().getId(),
          Status.ACTIVE,
          fileType, visibility, pageable);
    } else if (fileType != null) {
      page = repo.findAllByFamilyIdAndStatusAndFileType(ctx.familyId().getId(), Status.ACTIVE,
          fileType,
          pageable);
    } else if (visibility != null) {
      page = repo.findAllByFamilyIdAndStatusAndVisibility(ctx.familyId().getId(), Status.ACTIVE,
          visibility,
          pageable);
    } else {
      page = repo.findAllByFamilyIdAndStatus(ctx.familyId().getId(), Status.ACTIVE, pageable);
    }
    var visible = page.getContent().stream().filter(f -> {
      try {
        permission.assertCanAccess(f, ctx);
        return true;
      } catch (AppException ex) {
        return false;
      }
    }).toList();
    return new PageImpl<>(visible, pageable, visible.size());
  }
  
  public FileAsset get(UUID id, FamilyContext ctx) {
    FileAsset f = repo.findByIdAndFamilyIdAndStatus(id, ctx.familyId().getId(), Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "File not found"));
    permission.assertCanAccess(f, ctx);
    
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.FILE_VIEWED, "files", f.getId(),
        "{\"fileId\":\"" + id + "\"}");
    
    return f;
  }
  
  public Download download(UUID id, FamilyContext ctx) {
    FileAsset f = get(id, ctx);
    Resource r = storage.load(f.getStoragePath());
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.FILE_DOWNLOADED, "files", f.getId(),
        "{\"fileId\":\"" + id + "\"}");
    return new Download(f, r);
  }
  
  public FileAsset update(UUID id, FileDtos.UpdateFileRequest req, FamilyContext ctx) {
    FileAsset f = get(id, ctx);
    f.setCategory(req.category());
    f.setNotes(req.notes());
    if (req.visibility() != null) {
      f.setVisibility(req.visibility());
    }
    repo.save(f);
    
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.FILE_UPDATED, "files", f.getId(),
        "{\"fileId\":\"" + id + "\"}");
    return f;
  }
  
  public void delete(UUID id, FamilyContext ctx) {
    FileAsset f = get(id, ctx);
    f.setStatus(Status.DELETED);
    repo.save(f);
    
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.FILE_DELETED, "files", f.getId(),
        "{\"fileId\":\"" + id + "\"}");
  }
  
  public record Download(FileAsset file, Resource resource) {
  
  }
}
