package com.famora.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
  
  private final HttpStatus status;
  private final String code;
  
  public BusinessException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }
  
  public static BusinessException notFound(String message) {
    return new BusinessException(HttpStatus.NOT_FOUND, "BUSINESS_NOT_FOUND", message);
  }
  
  public static BusinessException forbidden(String message) {
    return new BusinessException(HttpStatus.FORBIDDEN, "BUSINESS_FORBIDDEN", message);
  }
  
  public static BusinessException validation(String message) {
    return new BusinessException(HttpStatus.BAD_REQUEST, "BUSINESS_VALIDATION_ERROR", message);
  }
  
  public static BusinessException conflict(String message) {
    return new BusinessException(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", message);
  }
}
