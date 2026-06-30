package com.famora.business.repository;

import com.famora.business.entity.Business;
import com.famora.common.helper.Status;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BusinessRepository extends JpaRepository<Business, UUID>,
    JpaSpecificationExecutor<Business> {
  
  Optional<Business> findByIdAndStatusNot(UUID id, Status status);
}
