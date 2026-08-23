package com.famora.vault.repository;

import com.famora.common.helper.Status;
import com.famora.vault.entity.VaultItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface VaultItemRepository extends JpaRepository<VaultItem, UUID>,
    JpaSpecificationExecutor<VaultItem> {
  
  Optional<VaultItem> findByIdAndFamilyIdAndStatus(UUID id, UUID familyId, Status status);

  @Query("select v from VaultItem v where v.encryptedSecret not like concat(:activePrefix, '%')")
  List<VaultItem> findForKeyRotation(String activePrefix, Pageable pageable);
  
}
