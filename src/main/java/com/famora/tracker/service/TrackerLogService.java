package com.famora.tracker.service;

import com.famora.audit.entity.AuditAction;
import com.famora.common.exception.BusinessException;
import com.famora.security.CurrentUserProvider;
import com.famora.tracker.dto.TrackerLogRequest;
import com.famora.tracker.dto.TrackerLogResponse;
import com.famora.tracker.entity.Tracker;
import com.famora.tracker.entity.TrackerLog;
import com.famora.tracker.repository.TrackerLogRepository;
import com.famora.tracker.spec.TrackerLogSpecifications;
import com.famora.user.entity.User;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackerLogService {
  
  private final TrackerLogRepository logRepository;
  private final TrackerService trackerService;
  private final TrackerAccessService accessService;
  private final CurrentUserProvider currentUserProvider;
  private final TrackerAuditService auditService;
  
  @Transactional
  public TrackerLogResponse create(UUID trackerId, TrackerLogRequest request) {
    User user = currentUserProvider.getCurrentUser();
    Tracker tracker = trackerService.requireTracker(trackerId);
    accessService.requireCanAccess(tracker, user);
    
    TrackerLog log = logRepository.findByTracker_IdAndLogDateAndLoggedByUser_Id(trackerId,
            request.logDate(), user.getId())
        .orElseGet(TrackerLog::new);
    boolean newLog = log.getId() == null;
    apply(log, tracker, user, request);
    TrackerLog saved = logRepository.save(log);
    auditService.log(tracker, newLog ? AuditAction.TRACKER_LOG_CREATED
        : AuditAction.TRACKER_LOG_UPDATED, "tracker_logs", saved.getId(),
        "{\"logDate\":\"" + saved.getLogDate() + "\"}");
    return TrackerLogResponse.from(saved);
  }
  
  @Transactional(readOnly = true)
  public Page<TrackerLogResponse> list(UUID trackerId, LocalDate fromDate, LocalDate toDate,
      Pageable pageable) {
    User user = currentUserProvider.getCurrentUser();
    Tracker tracker = trackerService.requireTracker(trackerId);
    accessService.requireCanAccess(tracker, user);
    return logRepository.findAll(TrackerLogSpecifications.tracker(trackerId)
            .and(TrackerLogSpecifications.logDateBetween(fromDate, toDate)),
        pageable).map(TrackerLogResponse::from);
  }
  
  @Transactional
  public TrackerLogResponse update(UUID trackerId, UUID logId, TrackerLogRequest request) {
    User user = currentUserProvider.getCurrentUser();
    Tracker tracker = trackerService.requireTracker(trackerId);
    accessService.requireCanAccess(tracker, user);
    TrackerLog log = requireLog(trackerId, logId);
    apply(log, tracker, user, request);
    TrackerLog saved = logRepository.save(log);
    auditService.log(tracker, AuditAction.TRACKER_LOG_UPDATED, "tracker_logs", saved.getId(),
        "{\"logDate\":\"" + saved.getLogDate() + "\"}");
    return TrackerLogResponse.from(saved);
  }
  
  @Transactional
  public void delete(UUID trackerId, UUID logId) {
    User user = currentUserProvider.getCurrentUser();
    Tracker tracker = trackerService.requireTracker(trackerId);
    accessService.requireCanAccess(tracker, user);
    TrackerLog log = requireLog(trackerId, logId);
    logRepository.delete(log);
    auditService.log(tracker, AuditAction.TRACKER_LOG_DELETED, "tracker_logs", logId, null);
  }
  
  private void apply(TrackerLog log, Tracker tracker, User user, TrackerLogRequest request) {
    log.setTracker(tracker);
    log.setScopeType(tracker.getScopeType());
    log.setFamily(tracker.getFamily());
    log.setBusiness(tracker.getBusiness());
    log.setLoggedByUser(user);
    log.setLogDate(request.logDate());
    log.setStatus(request.status());
    log.setValue(clean(request.value()));
    log.setNotes(clean(request.notes()));
  }
  
  private TrackerLog requireLog(UUID trackerId, UUID logId) {
    return logRepository.findById(logId)
        .filter(log -> log.getTracker().getId().equals(trackerId))
        .orElseThrow(() -> BusinessException.notFound("Tracker log not found"));
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
