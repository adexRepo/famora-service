package com.famora.emergency.service;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.common.exception.AppException;
import com.famora.common.helper.Status;
import com.famora.emergency.dto.EmergencyDtos.Request;
import com.famora.emergency.entity.EmergencyContact;
import com.famora.emergency.helper.EmergencyCategory;
import com.famora.emergency.repository.EmergencyContactRepository;
import com.famora.family.dto.FamilyContext;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {
  
  private final EmergencyContactRepository repo;
  private final AuditLogService audit;
  
  @Transactional
  public EmergencyContact create(Request req, FamilyContext ctx) {
    EmergencyContact e = new EmergencyContact();
    e.setFamily(ctx.family());
    e.setCreatedBy(ctx.user());
    apply(e, req);
    repo.save(e);
    
    audit.log(ctx.family(), ctx.user(), AuditAction.EMERGENCY_CONTACT_CREATED,
        "emergency_contacts", e.getId(), "{\"emergencyContactId\":\"" + e.getId() + "\"}");
    return e;
  }
  
  @Transactional(readOnly = true)
  public Page<EmergencyContact> list(FamilyContext ctx, String keyword, EmergencyCategory category,
      Pageable pageable) {
    Specification<EmergencyContact> spec = familyActive(ctx)
        .and(category(category))
        .and(nameKeyword(keyword));
    return repo.findAll(spec, pageable);
  }
  
  @Transactional(readOnly = true)
  public EmergencyContact get(UUID id, FamilyContext ctx) {
    return repo.findByIdAndFamilyIdAndStatus(id, ctx.family().getId(), Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Emergency contact not found"));
  }
  
  @Transactional
  public EmergencyContact update(UUID id, Request req, FamilyContext ctx) {
    EmergencyContact e = get(id, ctx);
    apply(e, req);
    e.setUpdatedBy(ctx.user());
    repo.save(e);
    
    audit.log(ctx.family(), ctx.user(), AuditAction.EMERGENCY_CONTACT_UPDATED,
        "emergency_contacts", e.getId(),
        "{\"emergencyContactId\":\"" + id + "\"}");
    return e;
  }
  
  @Transactional
  public void delete(UUID id, FamilyContext ctx) {
    EmergencyContact e = get(id, ctx);
    e.setStatus(Status.DELETED);
    e.setUpdatedBy(ctx.user());
    repo.save(e);
    
    audit.log(ctx.family(), ctx.user(), AuditAction.EMERGENCY_CONTACT_DELETED,
        "emergency_contacts", e.getId(), "{\"emergencyContactId\":\"" + id + "\"}");
  }
  
  private void apply(EmergencyContact e, Request req) {
    e.setName(clean(req.name()));
    e.setPhone(clean(req.phone()));
    e.setCategory(req.category());
    e.setLocation(clean(req.location()));
    e.setNotes(clean(req.notes()));
  }
  
  private Specification<EmergencyContact> familyActive(FamilyContext ctx) {
    return (root, query, cb) -> cb.and(
        cb.equal(root.get("family").get("id"), ctx.family().getId()),
        cb.equal(root.get("status"), Status.ACTIVE)
    );
  }
  
  private Specification<EmergencyContact> category(EmergencyCategory category) {
    return (root, query, cb) -> category == null
        ? cb.conjunction()
        : cb.equal(root.get("category"), category);
  }
  
  private Specification<EmergencyContact> nameKeyword(String keyword) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(keyword)) {
        return cb.conjunction();
      }
      return cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%");
    };
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
