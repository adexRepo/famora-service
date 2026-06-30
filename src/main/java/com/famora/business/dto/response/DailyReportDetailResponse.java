package com.famora.business.dto.response;

import java.util.List;

public record DailyReportDetailResponse(DailyReportSummaryResponse summary,
                                        List<SalesItemResponse> salesItems,
                                        List<PaymentBreakdownResponse> paymentBreakdowns,
                                        List<LossItemResponse> lossItems,
                                        List<ExpenseResponse> expenses) {}
