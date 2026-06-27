package com.famora.vault.repository;

import com.famora.vault.entity.VaultItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VaultItemRepository extends JpaRepository<VaultItem, UUID>,
    JpaSpecificationExecutor<VaultItem> {
  
  Optional<VaultItem> findByIdAndFamilyIdAndDeletedAtIsNull(UUID id, UUID familyId);
  
}
