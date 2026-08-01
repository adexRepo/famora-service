package com.famora.notification.service;

import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyLeaveRequest;
import com.famora.notification.dto.CreateNotificationCommand;
import com.famora.notification.enums.NotificationEntityType;
import com.famora.notification.enums.NotificationType;
import com.famora.tracker.enums.TrackerScopeType;
import com.famora.tracker.enums.TrackerSourceModule;
import com.famora.user.entity.User;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FamilyNotificationService {

  private final NotificationService notificationService;

  public void leaveRequested(FamilyLeaveRequest request, User owner) {
    notificationService.notifyUsers(List.of(owner.getId()), command(
        NotificationType.FAMILY_LEAVE_REQUESTED,
        "Family leave request",
        request.getRequester().getFullName() + " requested to leave "
            + request.getFamily().getName() + ".",
        request.getFamily(),
        NotificationEntityType.FAMILY_LEAVE_REQUEST,
        request.getId(),
        Map.of(
            "familyId", request.getFamily().getId(),
            "requestId", request.getId(),
            "requesterUserId", request.getRequester().getId()
        )));
  }

  public void leaveApproved(FamilyLeaveRequest request) {
    notificationService.notifyUsers(List.of(request.getRequester().getId()), command(
        NotificationType.FAMILY_LEAVE_APPROVED,
        "Leave request approved",
        "Your request to leave " + request.getFamily().getName() + " was approved.",
        request.getFamily(),
        NotificationEntityType.FAMILY_LEAVE_REQUEST,
        request.getId(),
        Map.of(
            "familyId", request.getFamily().getId(),
            "requestId", request.getId()
        )));
  }

  public void leaveRejected(FamilyLeaveRequest request) {
    notificationService.notifyUsers(List.of(request.getRequester().getId()), command(
        NotificationType.FAMILY_LEAVE_REJECTED,
        "Leave request rejected",
        "Your request to leave " + request.getFamily().getName() + " was rejected.",
        request.getFamily(),
        NotificationEntityType.FAMILY_LEAVE_REQUEST,
        request.getId(),
        Map.of(
            "familyId", request.getFamily().getId(),
            "requestId", request.getId()
        )));
  }

  public void ownershipTransferred(Family family, User oldOwner, User newOwner) {
    notificationService.notifyUsers(List.of(oldOwner.getId(), newOwner.getId()), command(
        NotificationType.FAMILY_OWNERSHIP_TRANSFERRED,
        "Family ownership transferred",
        newOwner.getFullName() + " is now owner of " + family.getName() + ".",
        family,
        NotificationEntityType.FAMILY,
        family.getId(),
        Map.of(
            "familyId", family.getId(),
            "oldOwnerUserId", oldOwner.getId(),
            "newOwnerUserId", newOwner.getId()
        )));
  }

  private CreateNotificationCommand command(NotificationType type, String title, String message,
      Family family, NotificationEntityType entityType, java.util.UUID entityId,
      Map<String, Object> payload) {
    return new CreateNotificationCommand(
        type,
        title,
        message,
        TrackerScopeType.FAMILY,
        family,
        null,
        TrackerSourceModule.FAMILY,
        entityType,
        entityId,
        null,
        payload
    );
  }
}
