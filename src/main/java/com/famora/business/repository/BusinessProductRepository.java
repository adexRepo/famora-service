package com.famora.business.repository;

import com.famora.business.entity.BusinessProduct;
import com.famora.common.helper.Status;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BusinessProductRepository extends JpaRepository<BusinessProduct, UUID>,
    JpaSpecificationExecutor<BusinessProduct> {
  
  Optional<BusinessProduct> findByIdAndBusinessIdAndStatus(UUID id, UUID businessId, Status status);
  
  Optional<BusinessProduct> findByIdAndBusinessIdAndStatusNot(UUID id, UUID businessId,
      Status status);
  
  Page<BusinessProduct> findByBusinessIdAndStatusNot(UUID businessId, Status status,
      Pageable pageable);
}
