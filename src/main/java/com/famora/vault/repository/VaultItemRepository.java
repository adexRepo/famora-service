package com.famora.vault.repository;

import com.famora.common.helper.Visibility;
import com.famora.vault.entity.VaultItem;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VaultItemRepository extends JpaRepository<VaultItem, UUID> {
  
  Optional<VaultItem> findByIdAndFamilyIdAndDeletedAtIsNull(UUID id, UUID familyId);
  
  @Query(
      value = """
        select v
        from VaultItem v
        where v.family.id = :familyId
          and v.deletedAt is null
          and v.visibility = :visibility
          and (
              v.visibility = com.famora.common.helper.Visibility.FAMILY
              or (
                  v.visibility = com.famora.common.helper.Visibility.PRIVATE
                  and v.createdBy.id = :userId
              )
              or (
                  :isOwner = true
                  and v.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
              )
          )
        order by v.createdAt desc
        """,
      countQuery = """
        select count(v)
        from VaultItem v
        where v.family.id = :familyId
          and v.deletedAt is null
          and v.visibility = :visibility
          and (
              v.visibility = com.famora.common.helper.Visibility.FAMILY
              or (
                  v.visibility = com.famora.common.helper.Visibility.PRIVATE
                  and v.createdBy.id = :userId
              )
              or (
                  :isOwner = true
                  and v.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
              )
          )
        """
  )
  Page<VaultItem> findVisibleByFamilyAndVisibility(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("visibility") Visibility visibility,
      Pageable pageable
  );
  
  
  @Query(
      value = """
        select v
        from VaultItem v
        where v.family.id = :familyId
          and v.deletedAt is null
          and v.visibility = :visibility
          and (
              lower(v.title) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(v.username, '')) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(v.url, '')) like lower(concat('%', :keyword, '%'))
          )
          and (
              v.visibility = com.famora.common.helper.Visibility.FAMILY
              or (
                  v.visibility = com.famora.common.helper.Visibility.PRIVATE
                  and v.createdBy.id = :userId
              )
              or (
                  :isOwner = true
                  and v.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
              )
          )
        order by v.createdAt desc
        """,
      countQuery = """
        select count(v)
        from VaultItem v
        where v.family.id = :familyId
          and v.deletedAt is null
          and v.visibility = :visibility
          and (
              lower(v.title) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(v.username, '')) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(v.url, '')) like lower(concat('%', :keyword, '%'))
          )
          and (
              v.visibility = com.famora.common.helper.Visibility.FAMILY
              or (
                  v.visibility = com.famora.common.helper.Visibility.PRIVATE
                  and v.createdBy.id = :userId
              )
              or (
                  :isOwner = true
                  and v.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
              )
          )
        """
  )
  Page<VaultItem> searchVisibleByFamilyAndKeywordAndVisibility(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("keyword") String keyword,
      @Param("visibility") Visibility visibility,
      Pageable pageable
  );
  
}
