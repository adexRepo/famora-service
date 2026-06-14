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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {
  
  private final EmergencyContactRepository repo;
  private final AuditLogService audit;
  
  public EmergencyContact create(Request req, FamilyContext ctx) {
    validate(req);
    EmergencyContact e = new EmergencyContact();
    e.setFamilyId(ctx.familyId().getId());
    e.setCreatedByUserId(ctx.userId().getId());
    apply(e, req);
    repo.save(e);
    
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.EMERGENCY_CONTACT_CREATED,
        "emergency_contacts", e.getId(), "{\"emergencyContactId\":\"" + e.getId() + "\"}");
    return e;
  }
  
  public Page<EmergencyContact> list(FamilyContext ctx, String keyword, EmergencyCategory category,
      Pageable pageable) {
    if (StringUtils.hasText(keyword)) {
      return repo.findAllByFamilyIdAndStatusAndNameContainingIgnoreCase(ctx.familyId().getId(),
          Status.ACTIVE, keyword, pageable);
    }
    if (category != null) {
      return repo.findAllByFamilyIdAndStatusAndCategory(ctx.familyId().getId(), Status.ACTIVE,
          category,
          pageable);
    }
    return repo.findAllByFamilyIdAndStatus(ctx.familyId().getId(), Status.ACTIVE, pageable);
  }
  
  public EmergencyContact get(UUID id, FamilyContext ctx) {
    return repo.findByIdAndFamilyIdAndStatus(id, ctx.familyId().getId(), Status.ACTIVE)
        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Emergency contact not found"));
  }
  
  public EmergencyContact update(UUID id, Request req, FamilyContext ctx) {
    validate(req);
    EmergencyContact e = get(id, ctx);
    apply(e, req);
    repo.save(e);
    
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.EMERGENCY_CONTACT_UPDATED,
        "emergency_contacts", e.getId(),
        "{\"emergencyContactId\":\"" + id + "\"}");
    return e;
  }
  
  public void delete(UUID id, FamilyContext ctx) {
    EmergencyContact e = get(id, ctx);
    e.setStatus(Status.DELETED);
    repo.save(e);
    
    audit.log(ctx.familyId(), ctx.userId(), AuditAction.EMERGENCY_CONTACT_DELETED,
        "emergency_contacts", e.getId(), "{\"emergencyContactId\":\"" + id + "\"}");
  }
  
  private void apply(EmergencyContact e, Request req) {
    e.setName(req.name());
    e.setPhone(req.phone());
    e.setCategory(req.category());
    e.setLocation(req.location());
    e.setNotes(req.notes());
  }
  
  private void validate(Request req) {
    if (req == null || !StringUtils.hasText(req.name())) {
      throw new AppException(HttpStatus.BAD_REQUEST, "name is required");
    }
    if (!StringUtils.hasText(req.phone())) {
      throw new AppException(HttpStatus.BAD_REQUEST, "phone is required");
    }
    if (req.category() == null) {
      throw new AppException(HttpStatus.BAD_REQUEST, "category is required");
    }
  }
}
