package com.famora.tracker.service;

import com.famora.audit.entity.AuditAction;
import com.famora.business.entity.BusinessMember;
import com.famora.common.exception.BusinessException;
import com.famora.family.entity.FamilyMember;
import com.famora.notification.service.NotificationScheduleService;
import com.famora.security.CurrentUserProvider;
import com.famora.tracker.dto.CreateTrackerRequest;
import com.famora.tracker.dto.TrackerResponse;
import com.famora.tracker.dto.TrackerTodayResponse;
import com.famora.tracker.dto.UpdateTrackerRequest;
import com.famora.tracker.entity.Tracker;
import com.famora.tracker.entity.TrackerLog;
import com.famora.tracker.enums.TrackerFrequency;
import com.famora.tracker.enums.TrackerLogStatus;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerStatus;
import com.famora.tracker.repository.TrackerLogRepository;
import com.famora.tracker.repository.TrackerRepository;
import com.famora.tracker.spec.TrackerSpecifications;
import com.famora.user.entity.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TrackerService {
  
  private final TrackerRepository trackerRepository;
  private final TrackerLogRepository logRepository;
  private final CurrentUserProvider currentUserProvider;
  private final TrackerAccessService accessService;
  private final RecurrenceCalculator recurrenceCalculator;
  private final NotificationScheduleService notificationScheduleService;
  private final TrackerAuditService auditService;
  
  @Transactional
  public TrackerResponse create(CreateTrackerRequest request) {
    User user = currentUserProvider.getCurrentUser();
    validateCreateUpdate(request.scopeType(), request.scopeId(), request.startDate(),
        request.dueDate(), request.recurrence(), request.notifyDelayMinutes());
    
    TrackerScopeContext scope = accessService.requireScopeAccess(request.scopeType(),
        request.scopeId(), user);
    Tracker tracker = new Tracker();
    apply(tracker, request, scope, user);
    tracker.setOwnerUser(user);
    tracker.setCreatedByUser(user);
    
    Tracker saved = trackerRepository.save(tracker);
    notificationScheduleService.regenerateForTracker(saved);
    auditService.log(saved, AuditAction.TRACKER_CREATED, "trackers", saved.getId(),
        "{\"title\":\"" + saved.getTitle() + "\"}");
    return TrackerResponse.from(saved);
  }
  
  @Transactional(readOnly = true)
  public Page<TrackerResponse> list(TrackerScopeType scopeType, UUID scopeId,
      com.famora.tracker.enums.TrackerType trackerType,
      com.famora.tracker.enums.TrackerCategory category,
      com.famora.tracker.enums.TrackerSourceModule sourceModule,
      TrackerStatus status,
      Pageable pageable) {
    User user = currentUserProvider.getCurrentUser();
    if (scopeType != null) {
      accessService.requireScopeAccess(scopeType, scopeId, user);
    }
    Specification<Tracker> spec = Specification
        .where(scopeType == null ? TrackerSpecifications.accessibleToUser(user.getId())
            : TrackerSpecifications.scope(scopeType, scopeId))
        .and(TrackerSpecifications.status(status))
        .and(TrackerSpecifications.type(trackerType))
        .and(TrackerSpecifications.category(category))
        .and(TrackerSpecifications.sourceModule(sourceModule));
    return trackerRepository.findAll(spec, pageable).map(TrackerResponse::from);
  }
  
  @Transactional(readOnly = true)
  public TrackerResponse get(UUID trackerId) {
    User user = currentUserProvider.getCurrentUser();
    Tracker tracker = requireTracker(trackerId);
    accessService.requireCanAccess(tracker, user);
    return TrackerResponse.from(tracker);
  }
  
  @Transactional
  public TrackerResponse update(UUID trackerId, UpdateTrackerRequest request) {
    User user = currentUserProvider.getCurrentUser();
    Tracker tracker = requireTracker(trackerId);
    accessService.requireCanAccess(tracker, user);
    validateCreateUpdate(request.scopeType(), request.scopeId(), request.startDate(),
        request.dueDate(), request.recurrence(), request.notifyDelayMinutes());
    TrackerScopeContext scope = accessService.requireScopeAccess(request.scopeType(),
        request.scopeId(), user);
    apply(tracker, request, scope, user);
    tracker.setUpdatedByUser(user);
    Tracker saved = trackerRepository.save(tracker);
    notificationScheduleService.regenerateForTracker(saved);
    auditService.log(saved, AuditAction.TRACKER_UPDATED, "trackers", saved.getId(), null);
    return TrackerResponse.from(saved);
  }
  
  @Transactional
  public void delete(UUID trackerId) {
    changeStatus(trackerId, TrackerStatus.DELETED, AuditAction.TRACKER_DELETED);
  }
  
  @Transactional
  public TrackerResponse pause(UUID trackerId) {
    return changeStatus(trackerId, TrackerStatus.PAUSED, AuditAction.TRACKER_PAUSED);
  }
  
  @Transactional
  public TrackerResponse resume(UUID trackerId) {
    TrackerResponse response = changeStatus(trackerId, TrackerStatus.ACTIVE,
        AuditAction.TRACKER_RESUMED);
    notificationScheduleService.regenerateForTracker(requireTracker(trackerId));
    return response;
  }
  
  @Transactional
  public TrackerResponse complete(UUID trackerId) {
    return changeStatus(trackerId, TrackerStatus.COMPLETED, AuditAction.TRACKER_COMPLETED);
  }
  
  @Transactional(readOnly = true)
  public List<TrackerTodayResponse> today(TrackerScopeType scopeType, UUID scopeId,
      LocalDate date) {
    User user = currentUserProvider.getCurrentUser();
    accessService.requireScopeAccess(scopeType, scopeId, user);
    LocalDate targetDate = date == null ? LocalDate.now() : date;
    List<Tracker> trackers = trackerRepository.findAll(
        TrackerSpecifications.scope(scopeType, scopeId)
            .and(TrackerSpecifications.status(TrackerStatus.ACTIVE))
            .and(TrackerSpecifications.startsOnOrBefore(targetDate)));
    
    return trackers.stream()
        .filter(tracker -> recurrenceCalculator.isDueOn(tracker, targetDate)
            || recurrenceCalculator.hasDueBefore(tracker, targetDate))
        .map(tracker -> toTodayResponse(tracker, targetDate, user.getId()))
        .toList();
  }
  
  public Tracker requireTracker(UUID trackerId) {
    return trackerRepository.findById(trackerId)
        .filter(tracker -> tracker.getStatus() != TrackerStatus.DELETED)
        .orElseThrow(() -> BusinessException.notFound("Tracker not found"));
  }
  
  private TrackerResponse changeStatus(UUID trackerId, TrackerStatus status, AuditAction action) {
    User user = currentUserProvider.getCurrentUser();
    Tracker tracker = requireTracker(trackerId);
    accessService.requireCanAccess(tracker, user);
    tracker.setStatus(status);
    tracker.setUpdatedByUser(user);
    Tracker saved = trackerRepository.save(tracker);
    if (status != TrackerStatus.ACTIVE) {
      notificationScheduleService.cancelFuturePending(saved);
    }
    auditService.log(saved, action, "trackers", saved.getId(), null);
    return TrackerResponse.from(saved);
  }
  
  private TrackerTodayResponse toTodayResponse(Tracker tracker, LocalDate date, UUID userId) {
    TrackerLog log = logRepository.findByTracker_IdAndLogDateAndLoggedByUser_Id(
        tracker.getId(), date, userId).orElse(null);
    TrackerLogStatus status = log == null ? TrackerLogStatus.PENDING : log.getStatus();
    return new TrackerTodayResponse(TrackerResponse.from(tracker), date,
        recurrenceCalculator.isDueOn(tracker, date),
        recurrenceCalculator.hasDueBefore(tracker, date),
        log == null ? null : log.getId(),
        status);
  }
  
  private void apply(Tracker tracker, CreateTrackerRequest request, TrackerScopeContext scope,
      User user) {
    tracker.setScopeType(request.scopeType());
    tracker.setFamily(scope.family());
    tracker.setBusiness(scope.business());
    tracker.setSourceModule(request.sourceModule());
    tracker.setSourceEntityType(clean(request.sourceEntityType()));
    tracker.setSourceEntityId(request.sourceEntityId());
    tracker.setTitle(request.title().trim());
    tracker.setDescription(clean(request.description()));
    tracker.setTrackerType(request.trackerType());
    tracker.setCategory(request.category());
    tracker.setAssignedUser(accessService.resolveAssignedUser(scope, request.assignedUserId()));
    FamilyMember familyMember = accessService.resolveAssignedFamilyMember(scope,
        request.scopeType() == TrackerScopeType.FAMILY ? request.assignedMemberId() : null);
    BusinessMember businessMember = accessService.resolveAssignedBusinessMember(scope,
        request.scopeType() == TrackerScopeType.BUSINESS ? request.assignedMemberId() : null);
    tracker.setAssignedFamilyMember(familyMember);
    tracker.setAssignedBusinessMember(businessMember);
    tracker.setStartDate(request.startDate());
    tracker.setDueDate(request.dueDate());
    tracker.setReminderTime(request.reminderTime());
    tracker.setTimezone(StringUtils.hasText(request.timezone()) ? request.timezone().trim()
        : "Asia/Kuala_Lumpur");
    tracker.setFrequency(request.recurrence().frequency());
    tracker.setIntervalValue(request.recurrence().interval() == null ? 1
        : request.recurrence().interval());
    tracker.setDaysOfWeek(toDaysOfWeek(request.recurrence().daysOfWeek()));
    tracker.setDayOfMonth(request.recurrence().dayOfMonth());
    tracker.setNotifyDelayMinutes(request.notifyDelayMinutes() == null ? 0
        : request.notifyDelayMinutes());
    tracker.setVisibility(request.visibility());
    tracker.setStatus(request.status() == null ? TrackerStatus.ACTIVE : request.status());
  }
  
  private void apply(Tracker tracker, UpdateTrackerRequest request, TrackerScopeContext scope,
      User user) {
    apply(tracker, new CreateTrackerRequest(request.scopeType(), request.scopeId(),
        request.sourceModule(), request.sourceEntityType(), request.sourceEntityId(),
        request.title(), request.description(), request.trackerType(), request.category(),
        request.assignedMemberId(), request.assignedUserId(), request.startDate(),
        request.dueDate(), request.reminderTime(), request.timezone(), request.recurrence(),
        request.notifyDelayMinutes(), request.visibility(), request.status()), scope, user);
  }
  
  private void validateCreateUpdate(TrackerScopeType scopeType, UUID scopeId, LocalDate startDate,
      LocalDate dueDate, com.famora.tracker.dto.RecurrenceRequest recurrence,
      Integer notifyDelayMinutes) {
    if (scopeType != TrackerScopeType.PERSONAL && scopeId == null) {
      throw BusinessException.validation("scopeId is required except PERSONAL tracker");
    }
    if (recurrence.frequency() == TrackerFrequency.ONCE && dueDate == null) {
      throw BusinessException.validation("dueDate is required for ONCE tracker");
    }
    if (recurrence.interval() != null && recurrence.interval() < 1) {
      throw BusinessException.validation("recurrence interval must be at least 1");
    }
    if (notifyDelayMinutes != null && notifyDelayMinutes < 0) {
      throw BusinessException.validation("notifyDelayMinutes must be greater than or equal to 0");
    }
    if (recurrence.frequency() == TrackerFrequency.WEEKLY
        && (recurrence.daysOfWeek() == null || recurrence.daysOfWeek().isEmpty())) {
      throw BusinessException.validation("daysOfWeek is required for WEEKLY tracker");
    }
    if (recurrence.frequency() == TrackerFrequency.MONTHLY
        && (recurrence.dayOfMonth() == null || recurrence.dayOfMonth() < 1
        || recurrence.dayOfMonth() > 31)) {
      throw BusinessException.validation("dayOfMonth must be 1-31 for MONTHLY tracker");
    }
    if (dueDate != null && startDate != null && dueDate.isBefore(startDate)
        && recurrence.frequency() != TrackerFrequency.ONCE) {
      throw BusinessException.validation("dueDate cannot be before startDate");
    }
  }
  
  private String toDaysOfWeek(List<DayOfWeek> daysOfWeek) {
    if (daysOfWeek == null || daysOfWeek.isEmpty()) {
      return null;
    }
    return daysOfWeek.stream().map(DayOfWeek::name).collect(Collectors.joining(","));
  }
  
  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
