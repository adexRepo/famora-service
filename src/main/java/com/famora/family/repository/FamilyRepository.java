package com.famora.family.repository;

import com.famora.common.helper.Status;
import com.famora.family.entity.Family;
import com.famora.user.entity.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FamilyRepository extends JpaRepository<Family, UUID> {

  boolean existsByOwnerUserAndStatus(User ownerUser, Status status);
}
