package com.famora.notification.service;

import com.famora.audit.entity.AuditAction;
import com.famora.common.exception.BusinessException;
import com.famora.notification.dto.CreateNotificationCommand;
import com.famora.notification.dto.NotificationUnreadCountResponse;
import com.famora.notification.dto.ScheduledNotificationResponse;
import com.famora.notification.entity.ScheduledNotification;
import com.famora.notification.enums.NotificationChannel;
import com.famora.notification.enums.NotificationDeliveryStatus;
import com.famora.notification.enums.NotificationReadStatus;
import com.famora.notification.repository.ScheduledNotificationRepository;
import com.famora.notification.spec.ScheduledNotificationSpecifications;
import com.famora.security.CurrentUserProvider;
import com.famora.tracker.service.TrackerAuditService;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class NotificationService {
  
  private final ScheduledNotificationRepository notificationRepository;
  private final CurrentUserProvider currentUserProvider;
  private final TrackerAuditService auditService;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;
  private final SimpMessagingTemplate messagingTemplate;
  private final NotificationPreferenceService preferenceService;
  
  @Transactional(readOnly = true)
  public Page<ScheduledNotificationResponse> list(NotificationDeliveryStatus deliveryStatus,
      NotificationReadStatus readStatus, Boolean read, String scope, Pageable pageable) {
    UUID userId = currentUserProvider.getCurrentUserId();
    Pageable sortedPageable = pageable.getSort().isSorted()
        ? pageable
        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
            Sort.by(Sort.Direction.DESC, "createdAt"));
    return notificationRepository.findAll(
        ScheduledNotificationSpecifications.receiver(userId)
            .and(ScheduledNotificationSpecifications.deliveryStatus(deliveryStatus))
            .and(ScheduledNotificationSpecifications.readStatus(readStatus))
            .and(ScheduledNotificationSpecifications.read(read))
            .and(ScheduledNotificationSpecifications.scope(parseScope(scope)))
            .and(ScheduledNotificationSpecifications.deliveryStatusNot(
                NotificationDeliveryStatus.CANCELLED)),
        sortedPageable).map(ScheduledNotificationResponse::from);
  }
  
  @Transactional(readOnly = true)
  public NotificationUnreadCountResponse unreadCount() {
    UUID userId = currentUserProvider.getCurrentUserId();
    return new NotificationUnreadCountResponse(
        notificationRepository.countByReceiverUser_IdAndReadStatusAndDeliveryStatusNot(userId,
            NotificationReadStatus.UNREAD, NotificationDeliveryStatus.CANCELLED));
  }
  
  @Transactional
  public ScheduledNotificationResponse markRead(UUID notificationId) {
    User user = currentUserProvider.getCurrentUser();
    ScheduledNotification notification = requireOwnNotification(notificationId, user.getId());
    if (notification.getReadStatus() != NotificationReadStatus.READ) {
      notification.setReadStatus(NotificationReadStatus.READ);
      notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
    notification = notificationRepository.save(notification);
    if (notification.getTracker() != null) {
      auditService.log(notification.getTracker(), AuditAction.NOTIFICATION_MARKED_READ,
          "scheduled_notifications", notification.getId(), null);
    }
    return ScheduledNotificationResponse.from(notification);
  }
  
  @Transactional
  public NotificationUnreadCountResponse markAllRead() {
    UUID userId = currentUserProvider.getCurrentUserId();
    notificationRepository.markAllReadByReceiverUserId(userId, NotificationReadStatus.UNREAD,
        NotificationReadStatus.READ, OffsetDateTime.now(ZoneOffset.UTC),
        NotificationDeliveryStatus.CANCELLED);
    return unreadCount();
  }
  
  @Transactional
  public void delete(UUID notificationId) {
    UUID userId = currentUserProvider.getCurrentUserId();
    ScheduledNotification notification = requireOwnNotification(notificationId, userId);
    notification.setDeliveryStatus(NotificationDeliveryStatus.CANCELLED);
    notification.setReadStatus(NotificationReadStatus.READ);
    notification.setReadAt(notification.getReadAt() == null ? OffsetDateTime.now(ZoneOffset.UTC)
        : notification.getReadAt());
    notificationRepository.save(notification);
  }
  
  @Transactional
  public List<ScheduledNotificationResponse> notifyUsers(Collection<UUID> receiverUserIds,
      CreateNotificationCommand command) {
    if (receiverUserIds == null || receiverUserIds.isEmpty()) {
      return List.of();
    }
    
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    List<User> receivers = userRepository.findAllById(receiverUserIds).stream()
        .filter(user -> user.getStatus() == UserStatus.ACTIVE)
        .filter(distinctById())
        .filter(user -> preferenceService.isInAppEnabled(user, command.type()))
        .toList();
    
    List<ScheduledNotification> notifications = receivers.stream()
        .map(receiver -> buildNotification(receiver, command, now))
        .toList();
    
    List<ScheduledNotificationResponse> responses = notificationRepository.saveAll(notifications)
        .stream()
        .map(ScheduledNotificationResponse::from)
        .toList();
    
    publishAfterCommit(responses);
    return responses;
  }
  
  private ScheduledNotification requireOwnNotification(UUID notificationId, UUID userId) {
    return notificationRepository.findById(notificationId)
        .filter(notification -> notification.getReceiverUser().getId().equals(userId))
        .orElseThrow(() -> BusinessException.notFound("Notification not found"));
  }
  
  private ScheduledNotification buildNotification(User receiver, CreateNotificationCommand command,
      OffsetDateTime now) {
    ScheduledNotification notification = new ScheduledNotification();
    notification.setReceiverUser(receiver);
    notification.setNotificationType(command.type());
    notification.setTitle(command.title());
    notification.setBody(command.message());
    notification.setScopeType(command.scope());
    notification.setFamily(command.family());
    notification.setBusiness(command.business());
    notification.setScheduledAt(command.scheduledAt() == null ? now : command.scheduledAt());
    notification.setChannel(NotificationChannel.IN_APP);
    notification.setDeliveryStatus(NotificationDeliveryStatus.SENT);
    notification.setReadStatus(NotificationReadStatus.UNREAD);
    notification.setSourceModule(command.sourceModule());
    notification.setSourceEntityType(command.entityType() == null ? null
        : command.entityType().name());
    notification.setEntityType(command.entityType());
    notification.setSourceEntityId(command.entityId());
    notification.setPayloadJson(objectMapper.valueToTree(command.payload() == null ? Map.of()
        : command.payload()));
    notification.setSentAt(now);
    return notification;
  }
  
  private com.famora.tracker.enums.TrackerScopeType parseScope(String scope) {
    if (scope == null || scope.isBlank()) {
      return null;
    }
    try {
      return com.famora.tracker.enums.TrackerScopeType.valueOf(scope.trim().toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw BusinessException.validation("Invalid notification scope");
    }
  }
  
  private java.util.function.Predicate<User> distinctById() {
    java.util.Set<UUID> seen = new java.util.HashSet<>();
    return user -> Objects.nonNull(user.getId()) && seen.add(user.getId());
  }
  
  private void publishAfterCommit(List<ScheduledNotificationResponse> responses) {
    if (responses.isEmpty()) {
      return;
    }
    Runnable publish = () -> responses.forEach(response -> messagingTemplate.convertAndSendToUser(
        response.receiverUserId().toString(), "/queue/notifications", response));
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      publish.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        publish.run();
      }
    });
  }
}
