package com.famora.common.exception;

public class ResourceNotFoundException extends RuntimeException {
  
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
