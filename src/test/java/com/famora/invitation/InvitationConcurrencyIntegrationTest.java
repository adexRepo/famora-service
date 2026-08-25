package com.famora.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import com.famora.business.entity.Business;
import com.famora.business.entity.BusinessInvitation;
import com.famora.business.enums.BusinessRole;
import com.famora.business.repository.BusinessInvitationRepository;
import com.famora.common.helper.Status;
import com.famora.family.entity.Family;
import com.famora.family.entity.FamilyInvitation;
import com.famora.family.helper.FamilyMemberRole;
import com.famora.family.helper.InvitationStatus;
import com.famora.family.repository.FamilyInvitationRepository;
import com.famora.user.entity.User;
import com.famora.user.entity.UserStatus;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
class InvitationConcurrencyIntegrationTest {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  @Autowired private EntityManager entityManager;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private FamilyInvitationRepository familyInvitationRepository;
  @Autowired private BusinessInvitationRepository businessInvitationRepository;

  @Test
  void familyInvitationCanOnlyBeConsumedByOneOfTwoUsers() throws Exception {
    Fixture fixture = createFixture();
    transactionTemplate.executeWithoutResult(status -> {
      FamilyInvitation invitation = FamilyInvitation.builder()
          .family(entityManager.getReference(Family.class, fixture.familyId()))
          .inviteCodeHash("a".repeat(64))
          .role(FamilyMemberRole.MEMBER)
          .status(InvitationStatus.ACTIVE)
          .expiresAt(OffsetDateTime.now().plusDays(1))
          .createdBy(entityManager.getReference(User.class, fixture.ownerId()))
          .build();
      entityManager.persist(invitation);
    });

    List<Boolean> results = race(
        () -> consumeFamily("a".repeat(64), fixture.firstUserId()),
        () -> consumeFamily("a".repeat(64), fixture.secondUserId()));

    assertThat(results).containsExactlyInAnyOrder(true, false);
  }

  @Test
  void businessInvitationCanOnlyBeConsumedByOneOfTwoUsers() throws Exception {
    Fixture fixture = createFixture();
    transactionTemplate.executeWithoutResult(status -> {
      Business business = entityManager.getReference(Business.class, fixture.businessId());
      User owner = entityManager.getReference(User.class, fixture.ownerId());
      BusinessInvitation invitation = new BusinessInvitation();
      invitation.setBusiness(business);
      invitation.setRole(BusinessRole.STAFF);
      invitation.setInvitationCodeHash("b".repeat(64));
      invitation.setInvitationStatus(com.famora.business.enums.InvitationStatus.PENDING);
      invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
      invitation.setInvitedByUserId(fixture.ownerId());
      invitation.setCreatedBy(owner);
      entityManager.persist(invitation);
    });

    List<Boolean> results = race(
        () -> consumeBusiness("b".repeat(64), fixture.firstUserId()),
        () -> consumeBusiness("b".repeat(64), fixture.secondUserId()));

    assertThat(results).containsExactlyInAnyOrder(true, false);
  }

  private boolean consumeFamily(String hash, UUID userId) {
    return Boolean.TRUE.equals(transactionTemplate.execute(status ->
        familyInvitationRepository.findByInviteCodeHashAndStatus(hash, InvitationStatus.ACTIVE)
            .map(invitation -> {
              pauseWhileHoldingLock();
              invitation.setStatus(InvitationStatus.USED);
              invitation.setUsedByUser(entityManager.getReference(User.class, userId));
              return true;
            }).orElse(false)));
  }

  private boolean consumeBusiness(String hash, UUID userId) {
    return Boolean.TRUE.equals(transactionTemplate.execute(status ->
        businessInvitationRepository.findByInvitationCodeHash(hash)
            .filter(invitation -> invitation.getInvitationStatus()
                == com.famora.business.enums.InvitationStatus.PENDING)
            .map(invitation -> {
              pauseWhileHoldingLock();
              invitation.setInvitationStatus(com.famora.business.enums.InvitationStatus.ACCEPTED);
              invitation.setAcceptedByUserId(userId);
              return true;
            }).orElse(false)));
  }

  private List<Boolean> race(Task first, Task second) throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<Boolean> firstResult = executor.submit(() -> runReady(ready, start, first));
      Future<Boolean> secondResult = executor.submit(() -> runReady(ready, start, second));
      ready.await();
      start.countDown();
      return List.of(firstResult.get(), secondResult.get());
    }
  }

  private boolean runReady(CountDownLatch ready, CountDownLatch start, Task task) throws Exception {
    ready.countDown();
    start.await();
    return task.run();
  }

  private void pauseWhileHoldingLock() {
    try {
      Thread.sleep(150);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ex);
    }
  }

  private Fixture createFixture() {
    return transactionTemplate.execute(status -> {
      User owner = user("owner-" + UUID.randomUUID() + "@example.com");
      User first = user("first-" + UUID.randomUUID() + "@example.com");
      User second = user("second-" + UUID.randomUUID() + "@example.com");
      entityManager.persist(owner);
      entityManager.persist(first);
      entityManager.persist(second);

      Family family = Family.builder().name("Family").ownerUser(owner).status(Status.ACTIVE).build();
      entityManager.persist(family);

      Business business = new Business();
      business.setName("Business");
      business.setOwnerUserId(owner.getId());
      business.setCreatedBy(owner);
      entityManager.persist(business);
      entityManager.flush();
      return new Fixture(owner.getId(), first.getId(), second.getId(), family.getId(),
          business.getId());
    });
  }

  private User user(String email) {
    return User.builder().fullName("User").email(email).passwordHash("hash")
        .status(UserStatus.ACTIVE).build();
  }

  @FunctionalInterface
  private interface Task {
    boolean run();
  }

  private record Fixture(UUID ownerId, UUID firstUserId, UUID secondUserId,
                         UUID familyId, UUID businessId) {
  }
}
