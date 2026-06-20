package com.famora.note.repository;

import com.famora.common.helper.Visibility;
import com.famora.note.entity.Note;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, UUID> {
  
  Optional<Note> findByIdAndFamilyIdAndDeletedAtIsNull(UUID id, UUID familyId);
  
  @Query(
      value = """
          select n
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          order by n.createdAt desc
          """,
      countQuery = """
          select count(n)
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          """
  )
  Page<Note> findVisibleByFamilyAndVisibility(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("visibility") Visibility visibility,
      Pageable pageable
  );
  
  @Query(
      value = """
          select n
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and lower(coalesce(n.category, '')) = lower(:category)
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          order by n.createdAt desc
          """,
      countQuery = """
          select count(n)
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and lower(coalesce(n.category, '')) = lower(:category)
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          """
  )
  Page<Note> findVisibleByFamilyAndVisibilityAndCategory(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("visibility") Visibility visibility,
      @Param("category") String category,
      Pageable pageable
  );
  
  @Query(
      value = """
          select n
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and (
                lower(n.title) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(n.content, '')) like lower(concat('%', :keyword, '%'))
            )
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          order by n.createdAt desc
          """,
      countQuery = """
          select count(n)
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and (
                lower(n.title) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(n.content, '')) like lower(concat('%', :keyword, '%'))
            )
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          """
  )
  Page<Note> searchVisibleByKeyword(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("visibility") Visibility visibility,
      @Param("keyword") String keyword,
      Pageable pageable
  );
  
  @Query(
      value = """
          select n
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and lower(coalesce(n.category, '')) = lower(:category)
            and (
                lower(n.title) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(n.content, '')) like lower(concat('%', :keyword, '%'))
            )
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          order by n.createdAt desc
          """,
      countQuery = """
          select count(n)
          from Note n
          where n.family.id = :familyId
            and n.status = com.famora.common.helper.Status.ACTIVE
            and n.visibility = :visibility
            and lower(coalesce(n.category, '')) = lower(:category)
            and (
                lower(n.title) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(n.content, '')) like lower(concat('%', :keyword, '%'))
            )
            and (
                n.visibility = com.famora.common.helper.Visibility.FAMILY
                or (
                    n.visibility = com.famora.common.helper.Visibility.PRIVATE
                    and n.createdBy.id = :userId
                )
                or (
                    :isOwner = true
                    and n.visibility = com.famora.common.helper.Visibility.OWNER_ONLY
                )
            )
          """
  )
  Page<Note> searchVisibleByKeywordAndCategory(
      @Param("familyId") UUID familyId,
      @Param("userId") UUID userId,
      @Param("isOwner") boolean isOwner,
      @Param("visibility") Visibility visibility,
      @Param("keyword") String keyword,
      @Param("category") String category,
      Pageable pageable
  );
  
}
