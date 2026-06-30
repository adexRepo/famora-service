package com.famora.business.repository;

import com.famora.business.entity.BusinessDailyLossItem;
import com.famora.business.entity.BusinessDailyPaymentBreakdown;
import com.famora.business.entity.BusinessDailySalesItem;
import com.famora.business.entity.BusinessExpense;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Query-only repository for building report snapshots without triggering N+1 queries.
 * <p>
 * This implementation intentionally uses EntityManager so it does not conflict with your existing
 * child repositories. If your project already has dedicated child repositories, moving these
 * methods there is also fine.
 */
@Repository
@RequiredArgsConstructor
public class BusinessDailyReportSnapshotQueryRepository {
  
  private final EntityManager entityManager;
  
  public List<BusinessDailySalesItem> findSalesItemsForSnapshot(UUID reportId) {
    return entityManager.createQuery("""
            select i
            from BusinessDailySalesItem i
            where i.dailyReportId = :reportId
            order by i.createdDt asc
            """, BusinessDailySalesItem.class)
        .setParameter("reportId", reportId)
        .getResultList();
  }
  
  public List<BusinessDailyPaymentBreakdown> findPaymentBreakdownsForSnapshot(UUID reportId) {
    return entityManager.createQuery("""
            select p
            from BusinessDailyPaymentBreakdown p
            where p.dailyReportId = :reportId
            order by p.createdDt asc
            """, BusinessDailyPaymentBreakdown.class)
        .setParameter("reportId", reportId)
        .getResultList();
  }
  
  public List<BusinessDailyLossItem> findLossItemsForSnapshot(UUID reportId) {
    return entityManager.createQuery("""
            select l
            from BusinessDailyLossItem l
            where l.dailyReportId = :reportId
            order by l.createdDt asc
            """, BusinessDailyLossItem.class)
        .setParameter("reportId", reportId)
        .getResultList();
  }
  
  public List<BusinessExpense> findExpensesForSnapshot(UUID reportId) {
    return entityManager.createQuery("""
            select e
            from BusinessExpense e
            where e.dailyReportId = :reportId
              and e.status <> com.famora.common.helper.Status.DELETED
            order by e.createdDt asc
            """, BusinessExpense.class)
        .setParameter("reportId", reportId)
        .getResultList();
  }
}
