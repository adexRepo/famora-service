package com.famora.tracker.repository;

import com.famora.tracker.entity.TrackerLog;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrackerLogRepository extends JpaRepository<TrackerLog, UUID>,
    JpaSpecificationExecutor<TrackerLog> {
  
  Optional<TrackerLog> findByTracker_IdAndLogDateAndLoggedByUser_Id(UUID trackerId,
      LocalDate logDate, UUID loggedByUserId);
}
