package com.famora.finance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class FinanceDashboardDtos {
  
  private FinanceDashboardDtos() {
  }
  
  public record DashboardResponse(
      String currency,
      BigDecimal currentEquity,
      Map<String, ChartResponse> cashflowChart,
      Map<String, CumulativeChartResponse> cumulativeTransactionChart,
      AllocationResponse allocation
  ) {
  
  }
  
  public record ChartResponse(
      int axisCount,
      BigDecimal maxAmount,
      @JsonProperty("yAxisLabels") List<BigDecimal> yaxisLabels,
      @JsonProperty("xAxisLabels") List<String> xaxisLabels,
      List<ChartPointResponse> points
  ) {
  
  }
  
  public record ChartPointResponse(
      LocalDate date,
      String label,
      BigDecimal amount
  ) {
  
  }
  
  public record CumulativeChartResponse(
      int axisCount,
      BigDecimal incomeChangePercent,
      BigDecimal expenseChangePercent,
      @JsonProperty("yAxisLabels") List<BigDecimal> yaxisLabels,
      @JsonProperty("xAxisLabels") List<String> xaxisLabels,
      List<CumulativePointResponse> points
  ) {
  
  }
  
  public record CumulativePointResponse(
      LocalDate date,
      String label,
      BigDecimal incomePercent,
      BigDecimal expensePercent
  ) {
  
  }
  
  public record AllocationResponse(
      BigDecimal totalAmount,
      List<AllocationItemResponse> items
  ) {
  
  }
  
  public record AllocationItemResponse(
      String type,
      BigDecimal amount,
      BigDecimal percentage
  ) {
  
  }
  
  public record EquityTransactionsResponse(
      String range,
      String periodMode,
      String currency,
      List<EquityTransactionRowResponse> rows,
      int limit,
      boolean hasMore
  ) {
  
  }
  
  public record EquityTransactionRowResponse(
      LocalDate date,
      String label,
      BigDecimal equity,
      BigDecimal income,
      BigDecimal expense
  ) {
  
  }
}
