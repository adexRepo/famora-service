package com.famora.business.service;

import com.famora.business.dto.response.BusinessTransactionResponse;
import com.famora.business.enums.BusinessTransactionType;
import com.famora.business.repository.BusinessTransactionQueryRepository;
import com.famora.security.CurrentUserProvider;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessTransactionService {
  
  private final BusinessPermissionService permissionService;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessTransactionQueryRepository transactionRepository;
  
  @Transactional(readOnly = true)
  public Page<BusinessTransactionResponse> list(UUID businessId, LocalDate fromDate,
      LocalDate toDate, BusinessTransactionType type, Pageable pageable) {
    permissionService.requireCanView(businessId, currentUserProvider.getCurrentUserId());
    return transactionRepository.findTransactions(businessId, fromDate, toDate, type, pageable);
  }
}
