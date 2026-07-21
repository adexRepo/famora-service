package com.famora.tracker.repository;

import com.famora.tracker.entity.Tracker;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TrackerRepository extends JpaRepository<Tracker, UUID>,
    JpaSpecificationExecutor<Tracker> {
}
