package com.famora.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.famora.audit.service.AuditLogService;
import com.famora.common.cache.TenantRedisCache;
import com.famora.currency.service.CurrencyConversionService;
import com.famora.family.dto.FamilyContext;
import com.famora.family.entity.Family;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.repository.FinanceTransactionRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

class FinanceServiceTest {

  private final FinanceTransactionRepository repository =
      mock(FinanceTransactionRepository.class);
  private final FinanceService service = new FinanceService(
      repository,
      mock(AuditLogService.class),
      mock(CurrencyConversionService.class),
      mock(TenantRedisCache.class)
  );

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void listSearchesDescriptionsWithinTheSelectedFamily() {
    UUID familyId = UUID.randomUUID();
    Family family = mock(Family.class);
    when(family.getId()).thenReturn(familyId);
    FamilyContext context = new FamilyContext(family, null, null, true);
    PageRequest pageable = PageRequest.of(0, 20);
    when(repository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(Page.empty(pageable));

    Page<?> result = service.list(
        context,
        "2026-08",
        "  Groceries_50%  ",
        null,
        null,
        pageable
    );

    assertThat(result).isEmpty();
    ArgumentCaptor<Specification<FinanceTransaction>> captor =
        ArgumentCaptor.forClass(Specification.class);
    verify(repository).findAll(captor.capture(), eq(pageable));

    Root<FinanceTransaction> root = mock(Root.class);
    CriteriaQuery<?> query = mock(CriteriaQuery.class);
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    Path familyPath = mock(Path.class);
    Path<UUID> familyIdPath = mock(Path.class);
    Path<String> description = mock(Path.class);
    Expression<String> coalescedDescription = mock(Expression.class);
    Expression<String> normalizedDescription = mock(Expression.class);
    when(root.get("family")).thenReturn(familyPath);
    when(familyPath.get("id")).thenReturn(familyIdPath);
    when(root.get("description")).thenReturn((Path) description);
    when(criteriaBuilder.coalesce(description, "")).thenReturn(coalescedDescription);
    when(criteriaBuilder.lower(coalescedDescription)).thenReturn(normalizedDescription);

    captor.getValue().toPredicate(root, query, criteriaBuilder);

    verify(criteriaBuilder).equal(familyIdPath, familyId);
    verify(criteriaBuilder).like(
        normalizedDescription,
        "%groceries\\_50\\%%",
        '\\'
    );
  }
}
