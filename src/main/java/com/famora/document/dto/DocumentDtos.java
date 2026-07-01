package com.famora.document.dto;

import com.famora.common.helper.Visibility;
import com.famora.document.entity.Document;
import com.famora.document.helper.DocumentCategory;
import com.famora.document.helper.DocumentType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public class DocumentDtos {
  
  public record DocumentResponse(UUID id, UUID fileId, UUID ownerUserId, String title,
                                 DocumentCategory documentCategory, DocumentType documentType,
                                 String documentNumber, LocalDate issueDate, LocalDate expiryDate,
                                 String notes, Visibility visibility, OffsetDateTime createdAt,
                                 OffsetDateTime updatedAt) {
    
    public static DocumentResponse from(Document d) {
      return new DocumentResponse(d.getId(), d.getFileId(), d.getOwnerUserId(), d.getTitle(),
          d.getDocumentType().getCategory(), d.getDocumentType(), d.getDocumentNumber(),
          d.getIssueDate(), d.getExpiryDate(), d.getNotes(), d.getVisibility(), d.getCreatedAt(),
          d.getUpdatedAt());
    }
  }
  
  public record UpdateDocumentRequest(String title, DocumentType documentType,
                                      String documentNumber, UUID ownerUserId, LocalDate issueDate,
                                      LocalDate expiryDate, String notes, Visibility visibility) {
    
  }
}
