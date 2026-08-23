package com.famora.notification.repository;

import com.famora.notification.entity.WebSocketTicket;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WebSocketTicketRepository extends JpaRepository<WebSocketTicket, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from WebSocketTicket t join fetch t.user where t.ticketHash = :ticketHash")
  Optional<WebSocketTicket> findByTicketHashForUpdate(String ticketHash);

  @Modifying
  @Query("delete from WebSocketTicket t where t.expiresAt < :cutoff or t.consumedAt is not null")
  int deleteExpiredOrConsumed(OffsetDateTime cutoff);
}
