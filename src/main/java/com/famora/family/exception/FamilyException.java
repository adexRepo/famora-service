package com.famora.family.exception;

import org.springframework.http.HttpStatus;

public class FamilyException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  public FamilyException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static FamilyException badRequest(String code, String message) {
    return new FamilyException(HttpStatus.BAD_REQUEST, code, message);
  }

  public static FamilyException forbidden(String code, String message) {
    return new FamilyException(HttpStatus.FORBIDDEN, code, message);
  }

  public static FamilyException notFound(String code, String message) {
    return new FamilyException(HttpStatus.NOT_FOUND, code, message);
  }

  public static FamilyException conflict(String code, String message) {
    return new FamilyException(HttpStatus.CONFLICT, code, message);
  }
}
