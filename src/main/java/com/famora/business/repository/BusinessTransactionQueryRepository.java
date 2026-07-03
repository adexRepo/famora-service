package com.famora.business.repository;

import com.famora.business.dto.response.BusinessTransactionResponse;
import com.famora.business.enums.BusinessTransactionType;
import com.famora.business.enums.PaymentMethod;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BusinessTransactionQueryRepository {
  
  private static final String BASE_QUERY = """
      select *
      from (
        select
          p.id as id,
          r.business_id as business_id,
          'DAILY_REPORT_PAYMENT' as source_type,
          p.id as source_id,
          r.id as daily_report_id,
          r.report_date as transaction_date,
          'INCOME' as transaction_type,
          concat('Daily Sales - ', r.report_date, ' ', r.shift, ' ', p.payment_method) as name,
          p.amount as amount,
          p.payment_method as payment_method,
          'SALES' as category,
          p.notes as notes,
          p.created_at as created_at
        from famora.business_daily_payment_breakdowns p
        join famora.business_daily_reports r on r.id = p.daily_report_id
        where r.business_id = :businessId
          and p.status = 'ACTIVE'
          and r.status = 'ACTIVE'
          and r.report_status <> 'VOIDED'
      
        union all
      
        select
          e.id as id,
          e.business_id as business_id,
          'BUSINESS_EXPENSE' as source_type,
          e.id as source_id,
          e.daily_report_id as daily_report_id,
          e.expense_date as transaction_date,
          'OUTCOME' as transaction_type,
          e.expense_name as name,
          e.amount as amount,
          e.payment_method as payment_method,
          e.category as category,
          e.notes as notes,
          e.created_at as created_at
        from famora.business_expenses e
        where e.business_id = :businessId
          and e.status = 'ACTIVE'
      ) tx
      where 1 = 1
      """;
  
  private final EntityManager entityManager;
  
  public Page<BusinessTransactionResponse> findTransactions(
      UUID businessId,
      LocalDate fromDate,
      LocalDate toDate,
      BusinessTransactionType type,
      Pageable pageable
  ) {
    String filter = buildFilter(fromDate, toDate, type);
    String contentSql = BASE_QUERY + filter + """
        order by tx.transaction_date desc, tx.created_at desc, tx.id desc
        offset :offset rows fetch first :limit rows only
        """;
    String countSql = "select count(*) from (" + BASE_QUERY + filter + ") counted";
    
    Query contentQuery = entityManager.createNativeQuery(contentSql);
    Query countQuery = entityManager.createNativeQuery(countSql);
    bindFilters(contentQuery, businessId, fromDate, toDate, type);
    bindFilters(countQuery, businessId, fromDate, toDate, type);
    contentQuery.setParameter("offset", pageable.getOffset());
    contentQuery.setParameter("limit", pageable.getPageSize());
    
    @SuppressWarnings("unchecked")
    List<Object[]> rows = contentQuery.getResultList();
    List<BusinessTransactionResponse> content = rows.stream()
        .map(this::toResponse)
        .toList();
    long total = ((Number) countQuery.getSingleResult()).longValue();
    return new PageImpl<>(content, pageable, total);
  }
  
  private String buildFilter(LocalDate fromDate, LocalDate toDate, BusinessTransactionType type) {
    StringBuilder filter = new StringBuilder();
    if (fromDate != null) {
      filter.append(" and tx.transaction_date >= :fromDate\n");
    }
    if (toDate != null) {
      filter.append(" and tx.transaction_date <= :toDate\n");
    }
    if (type != null) {
      filter.append(" and tx.transaction_type = :type\n");
    }
    return filter.toString();
  }
  
  private void bindFilters(Query query, UUID businessId, LocalDate fromDate, LocalDate toDate,
      BusinessTransactionType type) {
    query.setParameter("businessId", businessId);
    if (fromDate != null) {
      query.setParameter("fromDate", fromDate);
    }
    if (toDate != null) {
      query.setParameter("toDate", toDate);
    }
    if (type != null) {
      query.setParameter("type", type.name());
    }
  }
  
  private BusinessTransactionResponse toResponse(Object[] row) {
    return new BusinessTransactionResponse(
        uuid(row[0]),
        uuid(row[1]),
        text(row[2]),
        uuid(row[3]),
        uuid(row[4]),
        localDate(row[5]),
        BusinessTransactionType.valueOf(text(row[6])),
        text(row[7]),
        decimal(row[8]),
        row[9] == null ? null : PaymentMethod.valueOf(text(row[9])),
        text(row[10]),
        text(row[11]),
        offsetDateTime(row[12])
    );
  }
  
  private UUID uuid(Object value) {
    return value == null ? null : UUID.fromString(value.toString());
  }
  
  private String text(Object value) {
    return value == null ? null : value.toString();
  }
  
  private BigDecimal decimal(Object value) {
    if (value == null) {
      return BigDecimal.ZERO;
    }
    if (value instanceof BigDecimal number) {
      return number;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    return new BigDecimal(value.toString());
  }
  
  private LocalDate localDate(Object value) {
    if (value instanceof LocalDate date) {
      return date;
    }
    if (value instanceof Date date) {
      return date.toLocalDate();
    }
    return LocalDate.parse(value.toString());
  }
  
  private OffsetDateTime offsetDateTime(Object value) {
    if (value instanceof OffsetDateTime dateTime) {
      return dateTime;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
    return OffsetDateTime.parse(value.toString());
  }
}
