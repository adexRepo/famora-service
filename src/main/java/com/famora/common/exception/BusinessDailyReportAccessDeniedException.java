package com.famora.common.exception;

/**
 * Replace this with your existing ForbiddenException if Famora already has one.
 */
public class BusinessDailyReportAccessDeniedException extends RuntimeException {
  public BusinessDailyReportAccessDeniedException(String message) {
    super(message);
  }
}
