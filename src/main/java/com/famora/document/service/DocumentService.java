package com.famora.document.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.helper.PermissionHelper;
import com.famora.common.helper.Status;
import com.famora.common.helper.Visibility;
import com.famora.common.spec.VisibleFamilyScopedSpecifications;
import com.famora.document.dto.DocumentDtos;
import com.famora.document.entity.Document;
import com.famora.document.helper.DocumentCategory;
import com.famora.document.helper.DocumentType;
import com.famora.document.repository.DocumentRepository;
import com.famora.document.spec.DocumentSpecifications;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.file.entity.FileAsset;
import com.famora.file.repository.FileRepository;
import com.famora.file.service.FileService;
import com.famora.security.CurrentUserProvider;
import com.famora.security.FamilyContextService;
import com.famora.user.entity.User;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class DocumentService {
  
  private final DocumentRepository docs;
  private final FileService files;
  private final FileRepository fileRepo;
  private final AuditLogService audit;
  private final CurrentUserProvider currentUserProvider;
  private final FamilyContextService familyContextService;
  
  public Document create(MultipartFile file, String title, DocumentType documentType,
      String documentNumber, UUID ownerUserId, LocalDate issueDate, LocalDate expiryDate,
      String notes, Visibility visibility, FamilyContext ctx) {
    
    User user = currentUserProvider.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    if (title == null || title.isBlank()) {
      throw new AppException(HttpStatus.BAD_REQUEST, "title is required");
    }
    if (documentType == null) {
      throw new AppException(HttpStatus.BAD_REQUEST, "documentType is required");
    }
    
    FileAsset fa = files.upload(file, "DOCUMENT", notes,
        visibility == null ? Visibility.OWNER_ONLY : visibility, ctx, "documents");
    Document d = new Document();
    d.setFamily(ctx.family());
    d.setFileId(fa.getId());
    d.setOwnerUserId(ownerUserId == null ? ctx.user().getId() : ownerUserId);
    d.setTitle(title);
    d.setDocumentType(documentType);
    d.setDocumentNumber(documentNumber);
    d.setIssueDate(issueDate);
    d.setExpiryDate(expiryDate);
    d.setNotes(notes);
    d.setCreatedBy(user);
    d.setVisibility(visibility == null ? Visibility.OWNER_ONLY : visibility);
    docs.save(d);
    audit.log(family, user, AuditAction.DOCUMENT_CREATED, "documents", d.getId(),
        "{\"documentId\":\"" + d.getId() + "\",\"fileId\":\"" + fa.getId() + "\"}");
    return d;
  }
  
  @Transactional(readOnly = true)
  public Page<Document> list(
      FamilyContext ctx,
      DocumentCategory category,
      DocumentType type,
      Boolean expiringSoon,
      Integer days,
      Visibility visibility,
      Pageable pageable
  ) {
    UUID familyId = ctx.family().getId();
    UUID userId = ctx.user().getId();
    boolean isOwner = ctx.owner();
    
    Specification<Document> spec = Specification
        .where(VisibleFamilyScopedSpecifications.<Document>visibleToUser(
            familyId,
            userId,
            isOwner,
            Status.ACTIVE,
            visibility
        ))
        .and(DocumentSpecifications.documentCategory(category))
        .and(DocumentSpecifications.documentType(type))
        .and(DocumentSpecifications.expiringSoon(expiringSoon, days));
    
    return docs.findAll(spec, pageable);
  }
  
  public Document get(UUID id, FamilyContext ctx) {
    
    User user = currentUserProvider.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Document d = docs.findByIdAndFamilyIdAndStatus(id, ctx.family().getId(), Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Document not found"));
    PermissionHelper.assertCanAccess(d.getVisibility(), d.getStatus(), d.getCreatedBy().getId(), ctx);
    
    audit.log(family, user, AuditAction.DOCUMENT_VIEWED, "documents", d.getId(),
        "{\"documentId\":\"" + id + "\"}");
    return d;
  }
  
  public FileService.Download download(UUID id, FamilyContext ctx) {
    Document d = get(id, ctx);
    FileService.Download dl = files.download(d.getFileId(), ctx);
    audit.log(ctx.family(), ctx.user(), AuditAction.DOCUMENT_DOWNLOADED, "documents", d.getId(),
        "{\"documentId\":\"" + id + "\"}");
    return dl;
  }
  
  public Document update(UUID id, DocumentDtos.UpdateDocumentRequest req, FamilyContext ctx) {
    User user = currentUserProvider.getCurrentUser();
    Family family = familyContextService.getCurrentFamily();
    
    Document d = get(id, ctx);
    if (req.title() != null) {
      d.setTitle(req.title());
    }
    if (req.documentType() != null) {
      d.setDocumentType(req.documentType());
    }
    d.setDocumentNumber(req.documentNumber());
    d.setOwnerUserId(req.ownerUserId());
    d.setIssueDate(req.issueDate());
    d.setExpiryDate(req.expiryDate());
    d.setNotes(req.notes());
    if (req.visibility() != null) {
      d.setVisibility(req.visibility());
    }
    docs.save(d);
    
    audit.log(family, user, AuditAction.DOCUMENT_UPDATED, "documents", d.getId(),
        "{\"documentId\":\"" + id + "\"}");
    return d;
  }
  
  public void delete(UUID id, FamilyContext ctx) {
    Document d = get(id, ctx);
    d.setStatus(Status.DELETED);
    docs.save(d);
    fileRepo.findByIdAndFamilyIdAndStatus(d.getFileId(), ctx.family().getId(), Status.ACTIVE)
        .ifPresent(f -> {
          f.setStatus(Status.DELETED);
          fileRepo.save(f);
        });
    
    audit.log(ctx.family(), ctx.user(), AuditAction.DOCUMENT_DELETED, "documents", d.getId(),
        "{\"documentId\":\"" + id + "\"}");
  }
}
