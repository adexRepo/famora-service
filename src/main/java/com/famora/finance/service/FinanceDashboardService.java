package com.famora.finance.service;

import com.famora.common.helper.Status;
import com.famora.currency.service.CurrencyConversionService;
import com.famora.family.dto.FamilyContext;
import com.famora.finance.dto.FinanceDashboardDtos.AllocationItemResponse;
import com.famora.finance.dto.FinanceDashboardDtos.AllocationResponse;
import com.famora.finance.dto.FinanceDashboardDtos.ChartPointResponse;
import com.famora.finance.dto.FinanceDashboardDtos.ChartResponse;
import com.famora.finance.dto.FinanceDashboardDtos.CumulativeChartResponse;
import com.famora.finance.dto.FinanceDashboardDtos.CumulativePointResponse;
import com.famora.finance.dto.FinanceDashboardDtos.DashboardResponse;
import com.famora.finance.dto.FinanceDashboardDtos.EquityTransactionRowResponse;
import com.famora.finance.dto.FinanceDashboardDtos.EquityTransactionsResponse;
import com.famora.finance.entity.FinanceDebt;
import com.famora.finance.entity.FinanceTransaction;
import com.famora.finance.entity.FinanceTransactionType;
import com.famora.finance.helper.FinanceDebtStatus;
import com.famora.finance.helper.FinanceDebtType;
import com.famora.finance.helper.FinanceEquityPeriodMode;
import com.famora.finance.helper.FinanceEquityRange;
import com.famora.finance.repository.FinanceDebtRepository;
import com.famora.finance.repository.FinanceTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceDashboardService {
  
  private static final List<String> DASHBOARD_RANGES = List.of("1W", "1M", "3M", "YTD", "1Y",
      "ALL");
  private static final int EQUITY_TABLE_LIMIT = 30;
  
  private final FinanceTransactionRepository transactionRepository;
  private final FinanceDebtRepository debtRepository;
  private final FinanceService financeService;
  private final CurrencyConversionService currencyConversionService;
  
  @Transactional(readOnly = true)
  public DashboardResponse dashboard(FamilyContext ctx, String currency) {
    String targetCurrency = financeService.normalizeCurrency(currency);
    List<FinanceTransaction> transactions = transactionRepository
        .findAllByFamilyIdAndStatusOrderByTransactionDateAscCreatedAtAsc(ctx.family().getId(),
            Status.ACTIVE);
    LocalDate today = LocalDate.now();
    BigDecimal currentEquity = equityUntil(transactions, targetCurrency, today);
    
    Map<String, ChartResponse> cashflowChart = new LinkedHashMap<>();
    Map<String, CumulativeChartResponse> cumulativeChart = new LinkedHashMap<>();
    for (String range : DASHBOARD_RANGES) {
      RangeWindow window = window(range, today, firstTransactionDate(transactions, today));
      List<LocalDate> points = pointDates(range, window.start(), window.end());
      cashflowChart.put(range, cashflowChart(transactions, targetCurrency, range, points));
      cumulativeChart.put(range,
          cumulativeChart(transactions, targetCurrency, range, window, points));
    }
    
    return new DashboardResponse(
        targetCurrency,
        currentEquity,
        cashflowChart,
        cumulativeChart,
        allocation(ctx, transactions, targetCurrency, currentEquity, today)
    );
  }
  
  @Transactional(readOnly = true)
  public EquityTransactionsResponse equityTransactions(FamilyContext ctx, FinanceEquityRange range,
      FinanceEquityPeriodMode periodMode, String currency) {
    String targetCurrency = financeService.normalizeCurrency(currency);
    FinanceEquityRange cleanRange = range == null ? FinanceEquityRange.LAST_1_MONTH : range;
    FinanceEquityPeriodMode cleanMode = periodMode == null ? FinanceEquityPeriodMode.DAILY
        : periodMode;
    LocalDate today = LocalDate.now();
    LocalDate start = switch (cleanRange) {
      case LAST_1_MONTH -> today.minusMonths(1).plusDays(1);
      case LAST_3_MONTH -> today.minusMonths(3).plusDays(1);
      case LAST_6_MONTH -> today.minusMonths(6).plusDays(1);
    };
    
    List<FinanceTransaction> transactions = transactionRepository
        .findAllByFamilyIdAndStatusOrderByTransactionDateAscCreatedAtAsc(ctx.family().getId(),
            Status.ACTIVE);
    
    List<EquityTransactionRowResponse> allRows = cleanMode == FinanceEquityPeriodMode.MONTHLY
        ? monthlyRows(transactions, targetCurrency, start, today)
        : dailyRows(transactions, targetCurrency, start, today);
    
    boolean hasMore = allRows.size() > EQUITY_TABLE_LIMIT;
    List<EquityTransactionRowResponse> rows = hasMore
        ? allRows.subList(0, EQUITY_TABLE_LIMIT)
        : allRows;
    
    return new EquityTransactionsResponse(cleanRange.name(), cleanMode.name(), targetCurrency, rows,
        EQUITY_TABLE_LIMIT, hasMore);
  }
  
  private ChartResponse cashflowChart(List<FinanceTransaction> transactions, String currency,
      String range, List<LocalDate> pointDates) {
    List<ChartPointResponse> points = pointDates.stream()
        .map(date -> new ChartPointResponse(date, dateLabel(date, range),
            equityUntil(transactions, currency, date)))
        .toList();
    BigDecimal max = points.stream()
        .map(ChartPointResponse::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::max);
    int axisCount = axisCount(range);
    return new ChartResponse(axisCount, max, amountAxis(max, axisCount),
        xaxisLabels(pointDates, range, axisCount), points);
  }
  
  private CumulativeChartResponse cumulativeChart(List<FinanceTransaction> transactions,
      String currency, String range, RangeWindow window, List<LocalDate> pointDates) {
    BigDecimal totalIncome = sum(transactions, currency, window.start(), window.end(),
        FinanceTransactionType.INCOME);
    BigDecimal totalExpense = sum(transactions, currency, window.start(), window.end(),
        FinanceTransactionType.EXPENSE);
    
    List<CumulativePointResponse> points = pointDates.stream()
        .map(date -> new CumulativePointResponse(
            date,
            dateLabel(date, range),
            percent(sum(transactions, currency, window.start(), date, FinanceTransactionType.INCOME),
                totalIncome),
            percent(sum(transactions, currency, window.start(), date,
                FinanceTransactionType.EXPENSE), totalExpense)
        ))
        .toList();
    
    RangeWindow previous = previousWindow(window);
    BigDecimal previousIncome = sum(transactions, currency, previous.start(), previous.end(),
        FinanceTransactionType.INCOME);
    BigDecimal previousExpense = sum(transactions, currency, previous.start(), previous.end(),
        FinanceTransactionType.EXPENSE);
    
    int axisCount = axisCount(range);
    return new CumulativeChartResponse(axisCount, changePercent(totalIncome, previousIncome),
        changePercent(totalExpense, previousExpense), percentAxis(axisCount),
        xaxisLabels(pointDates, range, axisCount), points);
  }
  
  private AllocationResponse allocation(FamilyContext ctx, List<FinanceTransaction> transactions,
      String currency, BigDecimal currentEquity, LocalDate today) {
    YearMonth currentMonth = YearMonth.from(today);
    BigDecimal expenseThisMonth = sum(transactions, currency, currentMonth.atDay(1),
        currentMonth.atEndOfMonth(), FinanceTransactionType.EXPENSE);
    
    List<FinanceDebt> debts = debtRepository.findAllByFamilyIdAndStatus(ctx.family().getId(),
        Status.ACTIVE);
    BigDecimal payable = debts.stream()
        .filter(debt -> debt.getDebtType() == FinanceDebtType.PAYABLE)
        .filter(debt -> debt.getDebtStatus() != FinanceDebtStatus.CANCELLED)
        .map(debt -> convert(debt.getCurrency(), currency, debt.getRemainingAmount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal receivable = debts.stream()
        .filter(debt -> debt.getDebtType() == FinanceDebtType.RECEIVABLE)
        .filter(debt -> debt.getDebtStatus() != FinanceDebtStatus.CANCELLED)
        .map(debt -> convert(debt.getCurrency(), currency, debt.getRemainingAmount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal total = currentEquity.abs()
        .add(expenseThisMonth.abs())
        .add(payable.abs())
        .add(receivable.abs());
    
    List<AllocationItemResponse> items = List.of(
        allocationItem("EQUITY", currentEquity, total),
        allocationItem("EXPENSE", expenseThisMonth, total),
        allocationItem("PAYABLE", payable, total),
        allocationItem("RECEIVABLE", receivable, total)
    );
    
    return new AllocationResponse(total.setScale(2, RoundingMode.HALF_UP), items);
  }
  
  private List<EquityTransactionRowResponse> dailyRows(List<FinanceTransaction> transactions,
      String currency, LocalDate start, LocalDate end) {
    List<EquityTransactionRowResponse> rows = new ArrayList<>();
    for (LocalDate date = end; !date.isBefore(start); date = date.minusDays(1)) {
      rows.add(new EquityTransactionRowResponse(date, dateLabel(date, "1M"),
          equityUntil(transactions, currency, date),
          sum(transactions, currency, date, date, FinanceTransactionType.INCOME),
          sum(transactions, currency, date, date, FinanceTransactionType.EXPENSE)));
    }
    return rows;
  }
  
  private List<EquityTransactionRowResponse> monthlyRows(List<FinanceTransaction> transactions,
      String currency, LocalDate start, LocalDate end) {
    List<EquityTransactionRowResponse> rows = new ArrayList<>();
    YearMonth cursor = YearMonth.from(end);
    YearMonth first = YearMonth.from(start);
    while (!cursor.isBefore(first)) {
      LocalDate monthStart = cursor.atDay(1);
      LocalDate monthEnd = cursor.atEndOfMonth();
      LocalDate effectiveStart = monthStart.isBefore(start) ? start : monthStart;
      LocalDate effectiveEnd = monthEnd.isAfter(end) ? end : monthEnd;
      rows.add(new EquityTransactionRowResponse(effectiveEnd, monthLabel(cursor),
          equityUntil(transactions, currency, effectiveEnd),
          sum(transactions, currency, effectiveStart, effectiveEnd, FinanceTransactionType.INCOME),
          sum(transactions, currency, effectiveStart, effectiveEnd, FinanceTransactionType.EXPENSE)));
      cursor = cursor.minusMonths(1);
    }
    return rows;
  }
  
  private BigDecimal equityUntil(List<FinanceTransaction> transactions, String currency,
      LocalDate date) {
    BigDecimal income = sum(transactions, currency, null, date, FinanceTransactionType.INCOME);
    BigDecimal expense = sum(transactions, currency, null, date, FinanceTransactionType.EXPENSE);
    return income.subtract(expense).setScale(2, RoundingMode.HALF_UP);
  }
  
  private BigDecimal sum(List<FinanceTransaction> transactions, String currency, LocalDate start,
      LocalDate end, FinanceTransactionType type) {
    return transactions.stream()
        .filter(transaction -> transaction.getType() == type)
        .filter(transaction -> start == null || !transaction.getTransactionDate().isBefore(start))
        .filter(transaction -> end == null || !transaction.getTransactionDate().isAfter(end))
        .map(transaction -> convert(transaction.getCurrency(), currency, transaction.getAmount()))
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }
  
  private BigDecimal convert(String sourceCurrency, String targetCurrency, BigDecimal amount) {
    String source = financeService.normalizeCurrency(sourceCurrency);
    String target = financeService.normalizeCurrency(targetCurrency);
    BigDecimal cleanAmount = amount == null ? BigDecimal.ZERO : amount;
    if (source.equals(target)) {
      return cleanAmount.setScale(2, RoundingMode.HALF_UP);
    }
    return currencyConversionService.convert(source, target, cleanAmount)
        .setScale(2, RoundingMode.HALF_UP);
  }
  
  private RangeWindow window(String range, LocalDate today, LocalDate firstTransactionDate) {
    return switch (range) {
      case "1W" -> new RangeWindow(today.minusDays(6), today);
      case "1M" -> new RangeWindow(today.minusMonths(1).plusDays(1), today);
      case "3M" -> new RangeWindow(today.minusMonths(3).plusDays(1), today);
      case "YTD" -> new RangeWindow(LocalDate.of(today.getYear(), Month.JANUARY, 1), today);
      case "1Y" -> new RangeWindow(today.minusYears(1).plusDays(1), today);
      case "ALL" -> new RangeWindow(firstTransactionDate, today);
      default -> new RangeWindow(today.minusMonths(1).plusDays(1), today);
    };
  }
  
  private RangeWindow previousWindow(RangeWindow current) {
    long days = ChronoUnit.DAYS.between(current.start(), current.end()) + 1;
    LocalDate previousEnd = current.start().minusDays(1);
    return new RangeWindow(previousEnd.minusDays(days - 1), previousEnd);
  }
  
  private List<LocalDate> pointDates(String range, LocalDate start, LocalDate end) {
    if ("YTD".equals(range) || "1Y".equals(range) || "ALL".equals(range)) {
      return monthlyPointDates(start, end);
    }
    
    List<LocalDate> dates = new ArrayList<>();
    long days = Math.max(1, ChronoUnit.DAYS.between(start, end));
    long step = Math.max(1, days / 20);
    for (LocalDate cursor = start; !cursor.isAfter(end); cursor = cursor.plusDays(step)) {
      dates.add(cursor);
    }
    if (!dates.getLast().equals(end)) {
      dates.add(end);
    }
    return dates;
  }
  
  private List<LocalDate> monthlyPointDates(LocalDate start, LocalDate end) {
    List<LocalDate> dates = new ArrayList<>();
    YearMonth cursor = YearMonth.from(start);
    YearMonth last = YearMonth.from(end);
    while (!cursor.isAfter(last)) {
      LocalDate date = cursor.atEndOfMonth();
      dates.add(date.isAfter(end) ? end : date);
      cursor = cursor.plusMonths(1);
    }
    if (dates.isEmpty()) {
      dates.add(end);
    }
    return dates;
  }
  
  private List<String> xaxisLabels(List<LocalDate> dates, String range, int axisCount) {
    if (dates.isEmpty()) {
      return List.of();
    }
    if (dates.size() <= axisCount) {
      return dates.stream().map(date -> dateLabel(date, range)).toList();
    }
    
    List<String> labels = new ArrayList<>();
    int last = dates.size() - 1;
    for (int i = 0; i < axisCount; i++) {
      int index = Math.round(i * (last / (float) (axisCount - 1)));
      labels.add(dateLabel(dates.get(index), range));
    }
    return labels;
  }
  
  private String dateLabel(LocalDate date, String range) {
    if ("YTD".equals(range) || "1Y".equals(range) || "ALL".equals(range)) {
      return monthLabel(YearMonth.from(date));
    }
    return date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " "
        + date.getDayOfMonth();
  }
  
  private String monthLabel(YearMonth yearMonth) {
    return yearMonth.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
  }
  
  private int axisCount(String range) {
    return "1W".equals(range) || "1M".equals(range) || "3M".equals(range) ? 5 : 4;
  }
  
  private List<BigDecimal> amountAxis(BigDecimal max, int axisCount) {
    if (max.compareTo(BigDecimal.ZERO) <= 0) {
      return zeroAxis(axisCount);
    }
    List<BigDecimal> labels = new ArrayList<>();
    for (int i = 0; i < axisCount; i++) {
      BigDecimal value = max.multiply(BigDecimal.valueOf(axisCount - 1L - i))
          .divide(BigDecimal.valueOf(axisCount - 1L), 2, RoundingMode.HALF_UP);
      labels.add(value);
    }
    return labels;
  }
  
  private List<BigDecimal> percentAxis(int axisCount) {
    List<BigDecimal> labels = new ArrayList<>();
    for (int i = 0; i < axisCount; i++) {
      labels.add(BigDecimal.valueOf(100L * (axisCount - 1L - i))
          .divide(BigDecimal.valueOf(axisCount - 1L), 2, RoundingMode.HALF_UP));
    }
    return labels;
  }
  
  private List<BigDecimal> zeroAxis(int axisCount) {
    List<BigDecimal> labels = new ArrayList<>();
    for (int i = 0; i < axisCount; i++) {
      labels.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
    }
    return labels;
  }
  
  private BigDecimal percent(BigDecimal value, BigDecimal total) {
    if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.multiply(BigDecimal.valueOf(100))
        .divide(total, 2, RoundingMode.HALF_UP);
  }
  
  private BigDecimal changePercent(BigDecimal current, BigDecimal previous) {
    if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
      return current != null && current.compareTo(BigDecimal.ZERO) > 0
          ? BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP)
          : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return current.subtract(previous)
        .multiply(BigDecimal.valueOf(100))
        .divide(previous, 2, RoundingMode.HALF_UP);
  }
  
  private AllocationItemResponse allocationItem(String type, BigDecimal amount, BigDecimal total) {
    BigDecimal cleanAmount = amount == null ? BigDecimal.ZERO : amount.setScale(2,
        RoundingMode.HALF_UP);
    return new AllocationItemResponse(type, cleanAmount, percent(cleanAmount.abs(), total));
  }
  
  private LocalDate firstTransactionDate(List<FinanceTransaction> transactions, LocalDate fallback) {
    return transactions.isEmpty() ? fallback : transactions.getFirst().getTransactionDate();
  }
  
  private record RangeWindow(LocalDate start, LocalDate end) {
  
  }
}
