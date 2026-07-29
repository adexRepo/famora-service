package com.famora.auth.exception;

import org.springframework.security.core.AuthenticationException;

public class RefreshTokenAuthenticationException extends AuthenticationException {
  
  public RefreshTokenAuthenticationException(String message) {
    super(message);
  }
}
