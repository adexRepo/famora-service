package com.famora.vault.repository;

import com.famora.vault.entity.VaultItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VaultItemRepository extends JpaRepository<VaultItem, UUID> {
  
  List<VaultItem> findByFamilyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID familyId);
  
  Optional<VaultItem> findByIdAndFamilyIdAndDeletedAtIsNull(UUID id, UUID familyId);
  
  @Query("""
          select v
          from VaultItem v
          where v.family.id = :familyId
            and v.deletedAt is null
            and (
                lower(v.title) like concat('%', :keyword, '%')
                or lower(coalesce(v.username, '')) like concat('%', :keyword, '%')
                or lower(coalesce(v.url, '')) like concat('%', :keyword, '%')
            )
          order by v.createdAt desc
      """)
  List<VaultItem> searchByFamilyAndKeyword(UUID familyId, String keyword);
}
