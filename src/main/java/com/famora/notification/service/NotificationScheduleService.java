package com.famora.notification.service;

import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.repository.ScheduledNotificationRepository;
import com.famora.tracker.entity.Tracker;
import com.famora.tracker.enums.TrackerFrequency;
import com.famora.tracker.service.RecurrenceCalculator;
import com.famora.user.entity.User;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationScheduleService {
  
  private static final int RECURRING_GENERATION_DAYS = 30;
  
  private final ScheduledNotificationRepository notificationRepository;
  private final RecurrenceCalculator recurrenceCalculator;
  
  @Transactional(propagation = Propagation.MANDATORY)
  public void regenerateForTracker(Tracker tracker) {
    cancelFuturePending(tracker);
    if (tracker.getReminderTime() == null) {
      return;
    }
    
    User receiver = tracker.getAssignedUser() == null ? tracker.getOwnerUser()
        : tracker.getAssignedUser();
    List<LocalDate> occurrences = occurrencesToSchedule(tracker);
    
    List<ScheduledNotification> notifications = occurrences.stream()
        .map(date -> buildNotification(tracker, receiver, date))
        .filter(notification -> !notification.getScheduledAt().isBefore(OffsetDateTime.now()))
        .toList();
    notificationRepository.saveAll(notifications);
  }
  
  @Transactional(propagation = Propagation.MANDATORY)
  public void cancelFuturePending(Tracker tracker) {
    notificationRepository.cancelFuturePendingByTrackerId(tracker.getId(),
        NotificationDeliveryStatus.PENDING, NotificationDeliveryStatus.CANCELLED,
        OffsetDateTime.now());
  }
  
  private List<LocalDate> occurrencesToSchedule(Tracker tracker) {
    if (tracker.getFrequency() == TrackerFrequency.ONCE) {
      return tracker.getDueDate() == null ? List.of() : List.of(tracker.getDueDate());
    }
    LocalDate fromDate = LocalDate.now(ZoneId.of(tracker.getTimezone()));
    if (tracker.getStartDate().isAfter(fromDate)) {
      fromDate = tracker.getStartDate();
    }
    return recurrenceCalculator.occurrencesBetween(tracker, fromDate,
        fromDate.plusDays(RECURRING_GENERATION_DAYS));
  }
  
  private ScheduledNotification buildNotification(Tracker tracker, User receiver,
      LocalDate occurrenceDate) {
    ZoneId zoneId = ZoneId.of(tracker.getTimezone());
    OffsetDateTime scheduledAt = occurrenceDate
        .atTime(tracker.getReminderTime())
        .atZone(zoneId)
        .minusMinutes(tracker.getNotifyDelayMinutes())
        .toOffsetDateTime();
    
    ScheduledNotification notification = new ScheduledNotification();
    notification.setTracker(tracker);
    notification.setScopeType(tracker.getScopeType());
    notification.setFamily(tracker.getFamily());
    notification.setBusiness(tracker.getBusiness());
    notification.setReceiverUser(receiver);
    notification.setTitle(tracker.getTitle());
    notification.setBody(tracker.getDescription());
    notification.setScheduledAt(scheduledAt);
    notification.setSourceModule(tracker.getSourceModule());
    notification.setSourceEntityType(tracker.getSourceEntityType());
    notification.setSourceEntityId(tracker.getSourceEntityId());
    return notification;
  }
}
