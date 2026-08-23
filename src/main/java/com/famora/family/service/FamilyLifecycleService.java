package com.famora.family.service;

import static com.famora.family.constant.FamilyErrorCodes.CANNOT_LEAVE_LAST_FAMILY;
import static com.famora.family.constant.FamilyErrorCodes.CANNOT_TRANSFER_TO_SELF;
import static com.famora.family.constant.FamilyErrorCodes.FAMILY_MEMBER_NOT_FOUND;
import static com.famora.family.constant.FamilyErrorCodes.FORBIDDEN_FAMILY_ACTION;
import static com.famora.family.constant.FamilyErrorCodes.LEAVE_REQUEST_ALREADY_EXISTS;
import static com.famora.family.constant.FamilyErrorCodes.LEAVE_REQUEST_NOT_FOUND;
import static com.famora.family.constant.FamilyErrorCodes.LEAVE_REQUEST_NOT_PENDING;
import static com.famora.family.constant.FamilyErrorCodes.NOT_ELIGIBLE_NEW_OWNER;
import static com.famora.family.constant.FamilyErrorCodes.OWNER_TRANSFER_REQUIRED;

import com.famora.audit.entity.AuditAction;
import com.famora.audit.service.AuditLogService;
import com.famora.family.dto.FamilyLeaveRequestResponse;
import com.famora.family.dto.FamilySummaryResponse;
import com.famora.family.dto.LeaveFamilyRequest;
import com.famora.family.dto.LeaveFamilyResultResponse;
import com.famora.family.dto.OwnershipTransferRequest;
import com.famora.family.dto.OwnershipTransferResponse;
import com.famora.family.dto.ReviewLeaveRequest;
import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyLeaveRequest;
import com.famora.family.entity.FamilyMember;
import com.famora.family.exception.FamilyException;
import com.famora.family.helper.FamilyLeaveRequestStatus;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.FamilyMemberStatus;
import com.famora.family.repository.FamilyLeaveRequestRepository;
import com.famora.family.repository.FamilyMemberRepository;
import com.famora.family.repository.FamilyRepository;
import com.famora.notification.service.FamilyNotificationService;
import com.famora.security.CurrentUserProvider;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import com.famora.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FamilyLifecycleService {

  private final CurrentUserProvider currentUserProvider;
  private final FamilyRepository familyRepository;
  private final FamilyMemberRepository familyMemberRepository;
  private final FamilyLeaveRequestRepository leaveRequestRepository;
  private final FamilyMembershipPolicyService membershipPolicyService;
  private final FamilyNotificationService notificationService;
  private final AuditLogService auditLogService;
  private final UserRepository userRepository;

  @Transactional
  public FamilyLeaveRequestResponse requestLeave(UUID familyId, LeaveFamilyRequest request) {
    User user = currentUserProvider.getCurrentUser();
    FamilyMember member = requireActiveMember(familyId, user.getId());
    if (member.getRole() == FamilyMemberRole.OWNER) {
      throw FamilyException.conflict(
          OWNER_TRANSFER_REQUIRED,
          "Owner must transfer ownership before leaving family.");
    }
    ensureCanLeave(user);
    if (leaveRequestRepository.existsByFamilyIdAndRequesterIdAndRequestStatus(
        familyId, user.getId(), FamilyLeaveRequestStatus.PENDING)) {
      throw FamilyException.conflict(
          LEAVE_REQUEST_ALREADY_EXISTS,
          "Leave request already exists.");
    }

    FamilyLeaveRequest leaveRequest = FamilyLeaveRequest.builder()
        .family(member.getFamily())
        .requester(user)
        .requestStatus(FamilyLeaveRequestStatus.PENDING)
        .reason(trimToNull(request.reason()))
        .build();
    leaveRequest = leaveRequestRepository.save(leaveRequest);

    FamilyMember ownerMember = requireOwnerMember(familyId);
    audit(leaveRequest.getFamily(), user, AuditAction.FAMILY_LEAVE_REQUESTED,
        leaveRequest.getId());
    notificationService.leaveRequested(leaveRequest, ownerMember.getUser());
    return toResponse(leaveRequest);
  }

  @Transactional(readOnly = true)
  public Page<FamilyLeaveRequestResponse> listLeaveRequests(UUID familyId, Pageable pageable) {
    User user = currentUserProvider.getCurrentUser();
    requireOwner(familyId, user.getId());
    return leaveRequestRepository.findByFamilyId(familyId, pageable).map(this::toResponse);
  }

  @Transactional
  public LeaveFamilyResultResponse approve(UUID familyId, UUID requestId) {
    User user = currentUserProvider.getCurrentUser();
    requireOwner(familyId, user.getId());
    FamilyLeaveRequest request = requirePendingLeaveRequest(familyId, requestId);
    FamilyMember requesterMember = requireActiveMember(familyId, request.getRequester().getId());
    ensureCanLeave(request.getRequester());

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    requesterMember.setStatus(FamilyMemberStatus.LEFT);
    requesterMember.setDefaultFamily(false);
    requesterMember.setRemovedAt(now);
    familyMemberRepository.save(requesterMember);

    request.setRequestStatus(FamilyLeaveRequestStatus.APPROVED);
    request.setReviewedBy(user);
    request.setReviewedAt(now);
    request = leaveRequestRepository.save(request);

    FamilySummaryResponse newDefaultFamily = ensureDefaultFamily(request.getRequester().getId());
    audit(request.getFamily(), user, AuditAction.FAMILY_LEAVE_APPROVED, request.getId());
    audit(request.getFamily(), request.getRequester(), AuditAction.FAMILY_MEMBER_LEFT,
        requesterMember.getId());
    notificationService.leaveApproved(request);
    return new LeaveFamilyResultResponse(familyId, newDefaultFamily);
  }

  @Transactional
  public FamilyLeaveRequestResponse reject(UUID familyId, UUID requestId,
      ReviewLeaveRequest reviewRequest) {
    User user = currentUserProvider.getCurrentUser();
    requireOwner(familyId, user.getId());
    FamilyLeaveRequest request = requirePendingLeaveRequest(familyId, requestId);

    request.setRequestStatus(FamilyLeaveRequestStatus.REJECTED);
    request.setReviewReason(trimToNull(reviewRequest.reason()));
    request.setReviewedBy(user);
    request.setReviewedAt(OffsetDateTime.now(ZoneOffset.UTC));
    request = leaveRequestRepository.save(request);

    audit(request.getFamily(), user, AuditAction.FAMILY_LEAVE_REJECTED, request.getId());
    notificationService.leaveRejected(request);
    return toResponse(request);
  }

  @Transactional
  public FamilyLeaveRequestResponse cancel(UUID familyId, UUID requestId) {
    User user = currentUserProvider.getCurrentUser();
    FamilyLeaveRequest request = requirePendingLeaveRequest(familyId, requestId);
    if (!request.getRequester().getId().equals(user.getId())) {
      throw FamilyException.forbidden(
          FORBIDDEN_FAMILY_ACTION,
          "Only requester can cancel leave request.");
    }

    request.setRequestStatus(FamilyLeaveRequestStatus.CANCELLED);
    request.setCancelledBy(user);
    request.setCancelledAt(OffsetDateTime.now(ZoneOffset.UTC));
    request = leaveRequestRepository.save(request);

    audit(request.getFamily(), user, AuditAction.FAMILY_LEAVE_CANCELLED, request.getId());
    return toResponse(request);
  }

  @Transactional
  public OwnershipTransferResponse transferOwnership(UUID familyId,
      OwnershipTransferRequest request) {
    UUID currentOwnerId = currentUserProvider.getCurrentUserId();
    List<User> lockedUsers = userRepository.findAllByIdForUpdate(
        List.of(currentOwnerId, request.newOwnerUserId()));
    User currentOwner = lockedUsers.stream()
        .filter(user -> user.getId().equals(currentOwnerId)
            && user.getStatus() == UserStatus.ACTIVE)
        .findFirst()
        .orElseThrow(() -> FamilyException.forbidden(
            FORBIDDEN_FAMILY_ACTION, "Current owner is not active."));
    User lockedNewOwner = lockedUsers.stream()
        .filter(user -> user.getId().equals(request.newOwnerUserId())
            && user.getStatus() == UserStatus.ACTIVE)
        .findFirst()
        .orElseThrow(() -> FamilyException.badRequest(
            NOT_ELIGIBLE_NEW_OWNER, "New owner must be an active user."));
    FamilyMember oldOwnerMember = requireOwner(familyId, currentOwner.getId());
    if (currentOwner.getId().equals(request.newOwnerUserId())) {
      throw FamilyException.badRequest(
          CANNOT_TRANSFER_TO_SELF,
          "Cannot transfer ownership to yourself.");
    }

    FamilyMember newOwnerMember = familyMemberRepository.findByFamilyIdAndUserIdAndStatus(
            familyId, lockedNewOwner.getId(), FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> FamilyException.badRequest(
            NOT_ELIGIBLE_NEW_OWNER,
            "New owner must be an active family member."));

    if (request.leaveAfterTransfer()) {
      ensureCanLeave(currentOwner);
    }

    Family family = oldOwnerMember.getFamily();
    family.setOwnerUser(newOwnerMember.getUser());
    familyRepository.save(family);

    oldOwnerMember.setRole(FamilyMemberRole.MEMBER);
    newOwnerMember.setRole(FamilyMemberRole.OWNER);
    familyMemberRepository.saveAll(List.of(oldOwnerMember, newOwnerMember));

    boolean oldOwnerLeftFamily = false;
    FamilySummaryResponse newDefaultFamily = currentDefaultFamily(currentOwner.getId());
    if (request.leaveAfterTransfer()) {
      oldOwnerMember.setStatus(FamilyMemberStatus.LEFT);
      oldOwnerMember.setDefaultFamily(false);
      oldOwnerMember.setRemovedAt(OffsetDateTime.now(ZoneOffset.UTC));
      familyMemberRepository.save(oldOwnerMember);
      oldOwnerLeftFamily = true;
      newDefaultFamily = ensureDefaultFamily(currentOwner.getId());
      audit(family, currentOwner, AuditAction.FAMILY_MEMBER_LEFT, oldOwnerMember.getId());
    }

    audit(family, currentOwner, AuditAction.FAMILY_OWNERSHIP_TRANSFERRED, family.getId());
    notificationService.ownershipTransferred(family, currentOwner, newOwnerMember.getUser());
    return new OwnershipTransferResponse(
        familyId,
        currentOwner.getId(),
        newOwnerMember.getUser().getId(),
        oldOwnerMember.getRole(),
        oldOwnerLeftFamily,
        newDefaultFamily
    );
  }

  private FamilyMember requireActiveMember(UUID familyId, UUID userId) {
    return familyMemberRepository.findByFamilyIdAndUserIdAndStatus(
            familyId, userId, FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> FamilyException.notFound(
            FAMILY_MEMBER_NOT_FOUND,
            "Active family member not found."));
  }

  private FamilyMember requireOwner(UUID familyId, UUID userId) {
    FamilyMember member = requireActiveMember(familyId, userId);
    if (member.getRole() != FamilyMemberRole.OWNER) {
      throw FamilyException.forbidden(
          FORBIDDEN_FAMILY_ACTION,
          "Only family owner can perform this action.");
    }
    return member;
  }

  private FamilyMember requireOwnerMember(UUID familyId) {
    return familyMemberRepository.findByFamilyIdAndRoleAndStatus(
            familyId, FamilyMemberRole.OWNER, FamilyMemberStatus.ACTIVE)
        .orElseThrow(() -> FamilyException.notFound(
            FAMILY_MEMBER_NOT_FOUND,
            "Family owner not found."));
  }

  private FamilyLeaveRequest requirePendingLeaveRequest(UUID familyId, UUID requestId) {
    FamilyLeaveRequest request = leaveRequestRepository.findByIdAndFamilyId(requestId, familyId)
        .orElseThrow(() -> FamilyException.notFound(
            LEAVE_REQUEST_NOT_FOUND,
            "Leave request not found."));
    if (request.getRequestStatus() != FamilyLeaveRequestStatus.PENDING) {
      throw FamilyException.conflict(
          LEAVE_REQUEST_NOT_PENDING,
          "Leave request is not pending.");
    }
    return request;
  }

  private void ensureCanLeave(User user) {
    if (membershipPolicyService.activeFamilyCount(user) <= 1) {
      throw FamilyException.conflict(
          CANNOT_LEAVE_LAST_FAMILY,
          "Create or join another family first before leaving this family.");
    }
  }

  private FamilySummaryResponse ensureDefaultFamily(UUID userId) {
    List<FamilyMember> activeMembers = familyMemberRepository.findActiveFamiliesByUserId(userId);
    if (activeMembers.isEmpty()) {
      return null;
    }
    FamilyMember defaultMember = activeMembers.stream()
        .filter(FamilyMember::isDefaultFamily)
        .findFirst()
        .orElseGet(() -> {
          FamilyMember selected = activeMembers.get(0);
          familyMemberRepository.clearDefaultByUserIdExceptFamily(userId,
              selected.getFamily().getId());
          selected.setDefaultFamily(true);
          return familyMemberRepository.save(selected);
        });
    return toFamilySummary(defaultMember);
  }

  private FamilySummaryResponse currentDefaultFamily(UUID userId) {
    return familyMemberRepository.findByUserIdAndDefaultFamilyTrueAndStatus(
            userId, FamilyMemberStatus.ACTIVE)
        .map(this::toFamilySummary)
        .orElse(null);
  }

  private FamilySummaryResponse toFamilySummary(FamilyMember member) {
    return new FamilySummaryResponse(
        member.getFamily().getId(),
        member.getFamily().getName(),
        member.isDefaultFamily()
    );
  }

  private FamilyLeaveRequestResponse toResponse(FamilyLeaveRequest request) {
    User reviewedBy = request.getReviewedBy();
    User cancelledBy = request.getCancelledBy();
    return new FamilyLeaveRequestResponse(
        request.getId(),
        request.getFamily().getId(),
        request.getRequester().getId(),
        request.getRequester().getFullName(),
        request.getRequestStatus(),
        request.getReason(),
        request.getReviewReason(),
        reviewedBy == null ? null : reviewedBy.getId(),
        reviewedBy == null ? null : reviewedBy.getFullName(),
        request.getReviewedAt(),
        cancelledBy == null ? null : cancelledBy.getId(),
        cancelledBy == null ? null : cancelledBy.getFullName(),
        request.getCancelledAt(),
        request.getCreatedAt(),
        request.getUpdatedAt()
    );
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private void audit(Family family, User user, AuditAction action, UUID entityId) {
    auditLogService.log(family, user, action, "family_membership_lifecycle", entityId,
        "{\"familyId\":\"" + family.getId() + "\",\"entityId\":\"" + entityId + "\"}");
  }
}
